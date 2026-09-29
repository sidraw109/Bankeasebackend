package com.bankease.account.controller;

import com.bankease.account.dto.KycRequest;
import com.bankease.account.dto.UserResponse;
import com.bankease.account.repository.UserRepository;
import com.bankease.account.service.AccountService;
import com.bankease.common.dto.ApiResponse;
import com.bankease.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Account Management", description = "User profile and KYC management")
public class AccountController {

    private final AccountService accountService;
    private final UserRepository userRepository;

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
        return ResponseEntity.ok(ApiResponse.success(accountService.getUserProfile(userId)));
    }

    @PostMapping("/kyc")
    @Operation(summary = "Submit KYC documents")
    public ResponseEntity<ApiResponse<String>> submitKyc(
            @Valid @RequestBody KycRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
        String result = accountService.submitKyc(userId, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/kyc/{userId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve KYC for a user (Admin only)")
    public ResponseEntity<ApiResponse<String>> approveKyc(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String result = accountService.approveKyc(userId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get user by account number")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByAccountNumber(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getUserByAccountNumber(accountNumber)));
    }
}
