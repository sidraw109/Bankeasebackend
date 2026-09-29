package com.bankease.fraud.controller;

import com.bankease.account.repository.UserRepository;
import com.bankease.common.dto.ApiResponse;
import com.bankease.common.exception.ResourceNotFoundException;
import com.bankease.fraud.document.FraudAlert;
import com.bankease.fraud.dto.DisputeRequest;
import com.bankease.fraud.entity.DisputeTicket;
import com.bankease.fraud.repository.FraudAlertRepository;
import com.bankease.fraud.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fraud & Support", description = "Dispute tickets and fraud alerts")
public class FraudController {

    private final DisputeService disputeService;
    private final FraudAlertRepository fraudAlertRepository;
    private final UserRepository userRepository;

    @PostMapping("/disputes")
    @Operation(summary = "Create a new dispute ticket")
    public ResponseEntity<ApiResponse<DisputeTicket>> createDispute(
            @Valid @RequestBody DisputeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
        return ResponseEntity.ok(ApiResponse.success(
                "Dispute ticket created",
                disputeService.createDispute(userId, request)));
    }

    @GetMapping("/disputes")
    @Operation(summary = "Get user's dispute tickets")
    public ResponseEntity<ApiResponse<Page<DisputeTicket>>> getDisputes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
        return ResponseEntity.ok(ApiResponse.success(
                disputeService.getUserDisputes(userId, page, size)));
    }

    @PutMapping("/disputes/{ticketNumber}/resolve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPORT')")
    @Operation(summary = "Resolve a dispute ticket (Support/Admin)")
    public ResponseEntity<ApiResponse<DisputeTicket>> resolveDispute(
            @PathVariable String ticketNumber,
            @RequestParam String resolution,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "Ticket resolved",
                disputeService.resolveDispute(ticketNumber, resolution, userDetails.getUsername())));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get fraud alerts for an account (Admin)")
    public ResponseEntity<ApiResponse<Page<FraudAlert>>> getFraudAlerts(
            @RequestParam String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                fraudAlertRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber, PageRequest.of(page, size))));
    }
}
