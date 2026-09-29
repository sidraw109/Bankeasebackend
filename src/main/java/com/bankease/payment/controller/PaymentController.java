package com.bankease.payment.controller;

import com.bankease.account.repository.UserRepository;
import com.bankease.common.dto.ApiResponse;
import com.bankease.common.exception.ResourceNotFoundException;
import com.bankease.common.ratelimit.RateLimitService;
import com.bankease.payment.dto.InterBankTransferRequest;
import com.bankease.payment.dto.PaymentResponse;
import com.bankease.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payment Gateway", description = "Inter-bank transfers via NEFT/RTGS/IMPS")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final RateLimitService rateLimitService;

    @PostMapping("/inter-bank")
    @Operation(summary = "Initiate inter-bank transfer (NEFT/RTGS/IMPS)")
    public ResponseEntity<ApiResponse<PaymentResponse>> interBankTransfer(
            @Valid @RequestBody InterBankTransferRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        rateLimitService.checkPaymentRateLimit(userDetails.getUsername());
        String accountNumber = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getAccountNumber();
        PaymentResponse response = paymentService.initiateInterBankTransfer(accountNumber, request);
        return ResponseEntity.ok(ApiResponse.success("Payment initiated", response));
    }

    @GetMapping("/history")
    @Operation(summary = "Get payment history (paginated)")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        String accountNumber = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getAccountNumber();
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentHistory(accountNumber, page, size)));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get payment by order ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentByOrderId(orderId)));
    }
}
