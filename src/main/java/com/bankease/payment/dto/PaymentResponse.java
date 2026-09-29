package com.bankease.payment.dto;

import com.bankease.payment.entity.PaymentOrder;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private String orderId;
    private String accountNumber;
    private BigDecimal amount;
    private String currency;
    private PaymentOrder.PaymentType paymentType;
    private PaymentOrder.PaymentStatus status;
    private String gatewayTransactionId;
    private String beneficiaryName;
    private String beneficiaryAccountNumber;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
