package com.bankease.payment.service;

import com.bankease.account.entity.User;
import com.bankease.account.repository.UserRepository;
import com.bankease.common.exception.BusinessException;
import com.bankease.common.exception.DuplicateTransactionException;
import com.bankease.common.exception.InsufficientFundsException;
import com.bankease.common.exception.ResourceNotFoundException;
import com.bankease.common.util.AccountNumberGenerator;
import com.bankease.payment.dto.InterBankTransferRequest;
import com.bankease.payment.dto.PaymentResponse;
import com.bankease.payment.entity.PaymentOrder;
import com.bankease.payment.gateway.PaymentGatewayAdapter;
import com.bankease.payment.repository.PaymentOrderRepository;
import com.bankease.transaction.entity.Transaction;
import com.bankease.transaction.event.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final UserRepository userRepository;
    private final PaymentGatewayAdapter gatewayAdapter;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentResponse initiateInterBankTransfer(String accountNumber, InterBankTransferRequest request) {

        // Idempotency check
        if (paymentOrderRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            PaymentOrder existing = paymentOrderRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .orElseThrow();
            return mapToResponse(existing);
        }

        // Validate sender
        User sender = userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        if (sender.getStatus() != User.AccountStatus.ACTIVE) {
            throw new BusinessException("Account is not active", "ACCOUNT_NOT_ACTIVE");
        }

        // Validate RTGS minimum (₹2,00,000)
        if (request.getPaymentType() == PaymentOrder.PaymentType.INTER_BANK_RTGS
                && request.getAmount().compareTo(new BigDecimal("200000")) < 0) {
            throw new BusinessException("RTGS requires minimum ₹2,00,000", "RTGS_MIN_AMOUNT");
        }

        // Check balance
        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds for inter-bank transfer");
        }

        String orderId = "ORD-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 12);

        PaymentOrder order = PaymentOrder.builder()
                .orderId(orderId)
                .accountNumber(accountNumber)
                .amount(request.getAmount())
                .paymentType(request.getPaymentType())
                .status(PaymentOrder.PaymentStatus.PROCESSING)
                .beneficiaryAccountNumber(request.getBeneficiaryAccountNumber())
                .beneficiaryName(request.getBeneficiaryName())
                .beneficiaryIfsc(request.getBeneficiaryIfsc())
                .beneficiaryBankName(request.getBeneficiaryBankName())
                .transferMode(request.getPaymentType().name().replace("INTER_BANK_", ""))
                .description(request.getDescription())
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        order = paymentOrderRepository.save(order);

        // Debit sender
        int updated = userRepository.updateBalance(sender.getId(), request.getAmount().negate());
        if (updated == 0) {
            order.setStatus(PaymentOrder.PaymentStatus.FAILED);
            order.setFailureReason("Balance debit failed");
            paymentOrderRepository.save(order);
            throw new InsufficientFundsException("Balance debit failed");
        }

        // Call gateway
        PaymentGatewayAdapter.GatewayTransferRequest gatewayRequest = PaymentGatewayAdapter.GatewayTransferRequest.builder()
                .sourceAccount(accountNumber)
                .beneficiaryAccount(request.getBeneficiaryAccountNumber())
                .beneficiaryName(request.getBeneficiaryName())
                .beneficiaryIfsc(request.getBeneficiaryIfsc())
                .amount(request.getAmount())
                .currency("INR")
                .transferMode(order.getTransferMode())
                .description(request.getDescription())
                .referenceId(orderId)
                .build();

        PaymentGatewayAdapter.GatewayResponse gatewayResponse = gatewayAdapter.initiateTransfer(gatewayRequest);

        if (gatewayResponse.isSuccess()) {
            order.setStatus(PaymentOrder.PaymentStatus.SUCCESS);
            order.setGatewayTransactionId(gatewayResponse.getGatewayTransactionId());
            order.setGatewayResponse(gatewayResponse.getMessage());
            order.setCompletedAt(LocalDateTime.now());
            log.info("Inter-bank transfer SUCCESS: orderId={}", orderId);
        } else {
            // Refund on failure
            userRepository.updateBalance(sender.getId(), request.getAmount());
            order.setStatus(PaymentOrder.PaymentStatus.FAILED);
            order.setFailureReason(gatewayResponse.getMessage());
            log.error("Inter-bank transfer FAILED: orderId={}", orderId);
        }

        order = paymentOrderRepository.save(order);

        // Publish event for notification
        if (order.getStatus() == PaymentOrder.PaymentStatus.SUCCESS) {
            Transaction syntheticTxn = Transaction.builder()
                    .referenceNumber(orderId)
                    .senderAccountNumber(accountNumber)
                    .receiverAccountNumber(request.getBeneficiaryAccountNumber())
                    .amount(request.getAmount())
                    .type(Transaction.TransactionType.INTER_BANK_TRANSFER)
                    .status(Transaction.TransactionStatus.SUCCESS)
                    .description(request.getDescription())
                    .createdAt(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .build();
            eventPublisher.publishEvent(new TransactionCompletedEvent(this, syntheticTxn));
        }

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentHistory(String accountNumber, int page, int size) {
        return paymentOrderRepository
                .findByAccountNumberOrderByCreatedAtDesc(accountNumber, PageRequest.of(page, size))
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(String orderId) {
        PaymentOrder order = paymentOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));
        return mapToResponse(order);
    }

    private PaymentResponse mapToResponse(PaymentOrder order) {
        return PaymentResponse.builder()
                .id(order.getId())
                .orderId(order.getOrderId())
                .accountNumber(order.getAccountNumber())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .paymentType(order.getPaymentType())
                .status(order.getStatus())
                .gatewayTransactionId(order.getGatewayTransactionId())
                .beneficiaryName(order.getBeneficiaryName())
                .beneficiaryAccountNumber(order.getBeneficiaryAccountNumber())
                .description(order.getDescription())
                .createdAt(order.getCreatedAt())
                .completedAt(order.getCompletedAt())
                .build();
    }
}
