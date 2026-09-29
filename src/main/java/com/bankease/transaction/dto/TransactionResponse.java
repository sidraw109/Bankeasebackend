package com.bankease.transaction.dto;

import com.bankease.transaction.entity.Transaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {
    private Long id;
    private String referenceNumber;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private BigDecimal amount;
    private BigDecimal fee;
    private String currency;
    private Transaction.TransactionType type;
    private Transaction.TransactionStatus status;
    private String description;
    private String channel;
    private BigDecimal senderBalanceAfter;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
