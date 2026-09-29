package com.bankease.transaction.service;

import com.bankease.account.entity.User;
import com.bankease.account.repository.UserRepository;
import com.bankease.common.exception.*;
import com.bankease.common.util.AccountNumberGenerator;
import com.bankease.transaction.dto.TransactionResponse;
import com.bankease.transaction.dto.TransferRequest;
import com.bankease.transaction.entity.Transaction;
import com.bankease.transaction.event.TransactionCompletedEvent;
import com.bankease.transaction.event.TransactionFailedEvent;
import com.bankease.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final BigDecimal TRANSACTION_FEE_PERCENT = new BigDecimal("0.001"); // 0.1%
    private static final BigDecimal MAX_FREE_TRANSACTION = new BigDecimal("10000");

    @Transactional
    public TransactionResponse intraBankTransfer(String senderAccountNumber, TransferRequest request) {

        // 1. Idempotency check
        if (transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            Transaction existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .orElseThrow();
            if (existing.getStatus() == Transaction.TransactionStatus.SUCCESS) {
                return mapToResponse(existing);
            }
            throw new DuplicateTransactionException(request.getIdempotencyKey());
        }

        // 2. Validate sender
        User sender = userRepository.findByAccountNumber(senderAccountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Sender account not found"));

        if (sender.getStatus() != User.AccountStatus.ACTIVE) {
            throw new BusinessException("Sender account is not active", "ACCOUNT_NOT_ACTIVE");
        }

        // 3. Validate receiver
        if (senderAccountNumber.equals(request.getReceiverAccountNumber())) {
            throw new BusinessException("Cannot transfer to self", "SELF_TRANSFER");
        }

        User receiver = userRepository.findByAccountNumber(request.getReceiverAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver account not found"));

        if (receiver.getStatus() != User.AccountStatus.ACTIVE) {
            throw new BusinessException("Receiver account is not active", "RECEIVER_NOT_ACTIVE");
        }

        // 4. Calculate fee
        BigDecimal fee = calculateFee(request.getAmount());
        BigDecimal totalDebit = request.getAmount().add(fee);

        // 5. Check balance
        if (sender.getBalance().compareTo(totalDebit) < 0) {
            throw new InsufficientFundsException(
                    String.format("Insufficient funds. Available: ₹%.2f, Required: ₹%.2f",
                            sender.getBalance(), totalDebit));
        }

        // 6. Check daily limit
        BigDecimal newDailyUsed = sender.getDailyTransactionUsed().add(request.getAmount());
        if (newDailyUsed.compareTo(sender.getDailyTransactionLimit()) > 0) {
            throw new BusinessException(
                    "Daily transaction limit exceeded. Limit: ₹" + sender.getDailyTransactionLimit(),
                    "DAILY_LIMIT_EXCEEDED");
        }

        // 7. Create transaction record
        String referenceNumber = AccountNumberGenerator.generateTransactionRef();
        Transaction transaction = Transaction.builder()
                .referenceNumber(referenceNumber)
                .idempotencyKey(request.getIdempotencyKey())
                .senderAccountNumber(senderAccountNumber)
                .receiverAccountNumber(request.getReceiverAccountNumber())
                .amount(request.getAmount())
                .fee(fee)
                .type(Transaction.TransactionType.INTRA_BANK_TRANSFER)
                .status(Transaction.TransactionStatus.PROCESSING)
                .description(request.getDescription())
                .channel(request.getChannel())
                .senderBalanceBefore(sender.getBalance())
                .receiverBalanceBefore(receiver.getBalance())
                .build();

        transaction = transactionRepository.save(transaction);

        try {
            // 8. Debit sender
            int senderUpdated = userRepository.updateBalance(sender.getId(), totalDebit.negate());
            if (senderUpdated == 0) {
                throw new InsufficientFundsException("Balance update failed for sender");
            }

            // 9. Credit receiver
            userRepository.updateBalance(receiver.getId(), request.getAmount());

            // 10. Refresh balances (persistence context was cleared by the bulk
            //     updates above, so re-read to avoid writing stale balances back).
            sender = userRepository.findById(sender.getId()).orElseThrow();
            receiver = userRepository.findById(receiver.getId()).orElseThrow();

            // 11. Update daily limit usage on the freshly loaded sender
            sender.setDailyTransactionUsed(newDailyUsed);
            userRepository.save(sender);

            // 12. Update transaction as SUCCESS
            transaction.setStatus(Transaction.TransactionStatus.SUCCESS);
            transaction.setSenderBalanceAfter(sender.getBalance());
            transaction.setReceiverBalanceAfter(receiver.getBalance());
            transaction.setCompletedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);

            log.info("Transfer SUCCESS: {} -> {} Amount: ₹{} Ref: {}",
                    senderAccountNumber, request.getReceiverAccountNumber(),
                    request.getAmount(), referenceNumber);

            // 13. Publish event (async notification)
            eventPublisher.publishEvent(new TransactionCompletedEvent(this, transaction));

        } catch (Exception ex) {
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason(ex.getMessage());
            transactionRepository.save(transaction);
            log.error("Transfer FAILED: {}", ex.getMessage());
            eventPublisher.publishEvent(new TransactionFailedEvent(this, transaction, ex.getMessage()));
            throw ex;
        }

        return mapToResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(String accountNumber, int page, int size) {
        return transactionRepository
                .findBySenderAccountNumberOrReceiverAccountNumberOrderByCreatedAtDesc(
                        accountNumber, accountNumber,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReference(String referenceNumber) {
        Transaction transaction = transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "referenceNumber", referenceNumber));
        return mapToResponse(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByDateRange(
            String accountNumber, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByAccountNumberAndDateRange(accountNumber, startDate, endDate)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private BigDecimal calculateFee(BigDecimal amount) {
        if (amount.compareTo(MAX_FREE_TRANSACTION) <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(TRANSACTION_FEE_PERCENT).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .referenceNumber(t.getReferenceNumber())
                .senderAccountNumber(t.getSenderAccountNumber())
                .receiverAccountNumber(t.getReceiverAccountNumber())
                .amount(t.getAmount())
                .fee(t.getFee())
                .currency(t.getCurrency())
                .type(t.getType())
                .status(t.getStatus())
                .description(t.getDescription())
                .channel(t.getChannel())
                .senderBalanceAfter(t.getSenderBalanceAfter())
                .createdAt(t.getCreatedAt())
                .completedAt(t.getCompletedAt())
                .build();
    }
}
