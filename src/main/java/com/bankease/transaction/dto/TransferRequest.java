package com.bankease.transaction.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotBlank(message = "Receiver account number is required")
    private String receiverAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum transfer amount is ₹1")
    @DecimalMax(value = "1000000.00", message = "Maximum single transfer amount is ₹10,00,000")
    private BigDecimal amount;

    @Size(max = 200, message = "Description must be less than 200 characters")
    private String description;

    @NotBlank(message = "Idempotency key is required")
    @Size(min = 10, max = 100, message = "Idempotency key must be between 10 and 100 characters")
    private String idempotencyKey;

    private String channel = "NETBANKING";
}
