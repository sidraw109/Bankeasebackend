package com.bankease.transaction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions",
    indexes = {
        @Index(name = "idx_transaction_sender", columnList = "sender_account_number"),
        @Index(name = "idx_transaction_receiver", columnList = "receiver_account_number"),
        @Index(name = "idx_transaction_idempotency", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_transaction_created", columnList = "created_at")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String referenceNumber;

    @Column(nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, length = 30)
    private String senderAccountNumber;

    @Column(nullable = false, length = 30)
    private String receiverAccountNumber;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(precision = 20, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(length = 200)
    private String description;

    @Column(length = 500)
    private String failureReason;

    @Column(precision = 20, scale = 2)
    private BigDecimal senderBalanceBefore;

    @Column(precision = 20, scale = 2)
    private BigDecimal senderBalanceAfter;

    @Column(precision = 20, scale = 2)
    private BigDecimal receiverBalanceBefore;

    @Column(precision = 20, scale = 2)
    private BigDecimal receiverBalanceAfter;

    @Column
    private String channel; // NETBANKING, MOBILE, API

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime completedAt;

    public enum TransactionType {
        INTRA_BANK_TRANSFER, INTER_BANK_TRANSFER, DEPOSIT, WITHDRAWAL, PAYMENT, REFUND
    }

    public enum TransactionStatus {
        PENDING, PROCESSING, SUCCESS, FAILED, REVERSED
    }
}
