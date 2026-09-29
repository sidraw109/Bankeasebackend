package com.bankease.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String orderId;

    @Column(nullable = false, length = 30)
    private String accountNumber;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(length = 100)
    private String gatewayTransactionId;

    @Column(length = 200)
    private String gatewayResponse;

    @Column(length = 200)
    private String description;

    // For inter-bank transfers
    @Column(length = 50)
    private String beneficiaryAccountNumber;

    @Column(length = 100)
    private String beneficiaryName;

    @Column(length = 50)
    private String beneficiaryIfsc;

    @Column(length = 50)
    private String beneficiaryBankName;

    @Column(length = 50)
    private String transferMode; // NEFT, RTGS, IMPS, UPI

    @Column(length = 50)
    private String upiId;

    @Column(length = 100)
    private String idempotencyKey;

    @Column(length = 500)
    private String failureReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime completedAt;

    public enum PaymentType {
        INTER_BANK_NEFT, INTER_BANK_RTGS, INTER_BANK_IMPS, UPI, BILL_PAYMENT, MERCHANT
    }

    public enum PaymentStatus {
        INITIATED, PROCESSING, SUCCESS, FAILED, REFUNDED, REVERSED
    }
}
