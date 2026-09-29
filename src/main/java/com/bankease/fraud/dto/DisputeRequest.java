package com.bankease.fraud.dto;

import com.bankease.fraud.entity.DisputeTicket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DisputeRequest {

    @NotNull(message = "Dispute type is required")
    private DisputeTicket.DisputeType disputeType;

    @NotBlank(message = "Subject is required")
    @Size(min = 5, max = 100)
    private String subject;

    @NotBlank(message = "Description is required")
    @Size(min = 20, max = 2000)
    private String description;

    private String transactionReference;
}
