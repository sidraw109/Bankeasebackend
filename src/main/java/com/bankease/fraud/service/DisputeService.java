package com.bankease.fraud.service;

import com.bankease.account.entity.User;
import com.bankease.account.repository.UserRepository;
import com.bankease.common.exception.ResourceNotFoundException;
import com.bankease.fraud.dto.DisputeRequest;
import com.bankease.fraud.entity.DisputeTicket;
import com.bankease.fraud.repository.DisputeTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeTicketRepository disputeTicketRepository;
    private final UserRepository userRepository;

    @Transactional
    public DisputeTicket createDispute(Long userId, DisputeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String ticketNumber = generateTicketNumber();

        DisputeTicket ticket = DisputeTicket.builder()
                .ticketNumber(ticketNumber)
                .userId(userId)
                .accountNumber(user.getAccountNumber())
                .transactionReference(request.getTransactionReference())
                .disputeType(request.getDisputeType())
                .status(DisputeTicket.TicketStatus.OPEN)
                .priority(determinePriority(request.getDisputeType()))
                .subject(request.getSubject())
                .description(request.getDescription())
                .build();

        ticket = disputeTicketRepository.save(ticket);
        log.info("Dispute ticket created: {} for user: {}", ticketNumber, userId);
        return ticket;
    }

    private String generateTicketNumber() {
        String prefix = "TKT-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        long sequence = disputeTicketRepository.countByTicketNumberStartingWith(prefix) + 1;
        String candidate = prefix + (1000 + sequence);
        while (disputeTicketRepository.findByTicketNumber(candidate).isPresent()) {
            sequence++;
            candidate = prefix + (1000 + sequence);
        }
        return candidate;
    }

    @Transactional
    public DisputeTicket resolveDispute(String ticketNumber, String resolution, String resolvedBy) {
        DisputeTicket ticket = disputeTicketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "number", ticketNumber));

        ticket.setStatus(DisputeTicket.TicketStatus.RESOLVED);
        ticket.setResolution(resolution);
        ticket.setAssignedTo(resolvedBy);
        ticket.setResolvedAt(LocalDateTime.now());

        return disputeTicketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public Page<DisputeTicket> getUserDisputes(Long userId, int page, int size) {
        return disputeTicketRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public DisputeTicket getDisputeByTicketNumber(String ticketNumber) {
        return disputeTicketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "number", ticketNumber));
    }

    private DisputeTicket.Priority determinePriority(DisputeTicket.DisputeType type) {
        return switch (type) {
            case UNAUTHORIZED_TRANSACTION, ACCOUNT_HACKED -> DisputeTicket.Priority.CRITICAL;
            case WRONG_AMOUNT, DUPLICATE_CHARGE -> DisputeTicket.Priority.HIGH;
            case TRANSACTION_NOT_RECEIVED -> DisputeTicket.Priority.MEDIUM;
            default -> DisputeTicket.Priority.LOW;
        };
    }
}
