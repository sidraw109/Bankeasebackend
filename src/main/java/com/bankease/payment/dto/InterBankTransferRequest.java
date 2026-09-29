package com.bankease.payment.dto;

import com.bankease.payment.entity.PaymentOrder;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InterBankTransferRequest {

    @NotBlank(message = "Beneficiary account number is required")
    private String beneficiaryAccountNumber;

    @NotBlank(message = "Beneficiary name is required")
    @Size(min = 2, max = 100)
    private String beneficiaryName;

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code format")
    private String beneficiaryIfsc;

    @NotBlank(message = "Beneficiary bank name is required")
    private String beneficiaryBankName;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum amount is ₹1")
    @DecimalMax(value = "10000000.00", message = "Maximum amount is ₹1 Crore")
    private BigDecimal amount;

    @NotNull(message = "Payment type is required")
    private PaymentOrder.PaymentType paymentType;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @Size(max = 200)
    private String description;
}
