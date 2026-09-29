package com.bankease.account.dto;

import com.bankease.account.entity.User;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String accountNumber;
    private BigDecimal balance;
    private User.AccountType accountType;
    private User.AccountStatus status;
    private Boolean kycVerified;
    private BigDecimal dailyTransactionLimit;
    private BigDecimal dailyTransactionUsed;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
