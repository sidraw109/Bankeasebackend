package com.bankease.account.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class KycRequest {

    @NotBlank(message = "Document type is required")
    @Pattern(regexp = "AADHAAR|PAN|PASSPORT|VOTER_ID", message = "Invalid document type")
    private String documentType;

    @NotBlank(message = "Document number is required")
    @Size(min = 5, max = 50, message = "Document number must be between 5 and 50 characters")
    private String documentNumber;

    @NotBlank(message = "Document image URL is required")
    private String documentImageUrl;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Gender must be MALE, FEMALE, or OTHER")
    private String gender;

    @NotBlank(message = "Address is required")
    @Size(min = 10, max = 500, message = "Address must be between 10 and 500 characters")
    private String address;

    @NotBlank(message = "Pin code is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Pin code must be a valid 6-digit code")
    private String pinCode;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;
}
