package com.bankease.transaction.controller;

import com.bankease.account.repository.UserRepository;
import com.bankease.common.dto.ApiResponse;
import com.bankease.common.exception.ResourceNotFoundException;
import com.bankease.common.ratelimit.RateLimitService;
import com.bankease.transaction.dto.TransactionResponse;
import com.bankease.transaction.dto.TransferRequest;
import com.bankease.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Transaction Management", description = "Intra-bank transfers and transaction history")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final RateLimitService rateLimitService;

    @PostMapping("/transfer")
    @Operation(summary = "Initiate intra-bank transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        rateLimitService.checkGeneralRateLimit(userDetails.getUsername());
        String accountNumber = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getAccountNumber();
        TransactionResponse response = transactionService.intraBankTransfer(accountNumber, request);
        return ResponseEntity.ok(ApiResponse.success("Transfer initiated successfully", response));
    }

    @GetMapping("/history")
    @Operation(summary = "Get transaction history (paginated)")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        String accountNumber = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getAccountNumber();
        Page<TransactionResponse> history = transactionService.getTransactionHistory(accountNumber, page, size);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/{referenceNumber}")
    @Operation(summary = "Get transaction by reference number")
    public ResponseEntity<ApiResponse<TransactionResponse>> getByReference(
            @PathVariable String referenceNumber) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getTransactionByReference(referenceNumber)));
    }

    @GetMapping("/statement")
    @Operation(summary = "Get account statement by date range")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getStatement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        String accountNumber = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getAccountNumber();
        List<TransactionResponse> statement = transactionService.getTransactionsByDateRange(
                accountNumber, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(statement));
    }
}
