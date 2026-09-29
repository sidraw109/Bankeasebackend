package com.bankease.notification.controller;

import com.bankease.account.repository.UserRepository;
import com.bankease.common.dto.ApiResponse;
import com.bankease.common.exception.ResourceNotFoundException;
import com.bankease.notification.document.Notification;
import com.bankease.notification.document.OtpRecord;
import com.bankease.notification.service.NotificationService;
import com.bankease.notification.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notification & OTP", description = "Notifications and OTP management")
public class NotificationController {

    private final NotificationService notificationService;
    private final OtpService otpService;
    private final UserRepository userRepository;

    // Notifications are keyed by account number (see NotificationEventListener).
    private String accountNumber(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getAccountNumber();
    }

    @PostMapping("/otp/send")
    @Operation(summary = "Send OTP to phone number")
    public ResponseEntity<ApiResponse<String>> sendOtp(
            @RequestParam @NotBlank String phone,
            @RequestParam OtpRecord.OtpPurpose purpose) {
        String result = otpService.generateAndSendOtp(phone, purpose, false);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify OTP")
    public ResponseEntity<ApiResponse<String>> verifyOtp(
            @RequestParam @NotBlank String phone,
            @RequestParam @NotBlank String otpCode,
            @RequestParam OtpRecord.OtpPurpose purpose) {
        boolean verified = otpService.verifyOtp(phone, otpCode, purpose, false);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all notifications (paginated)")
    public ResponseEntity<ApiResponse<Page<Notification>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = accountNumber(userDetails);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUserNotifications(userId, page, size)));
    }

    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications")
    public ResponseEntity<ApiResponse<Page<Notification>>> getUnread(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = accountNumber(userDetails);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadNotifications(userId, page, size)));
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = accountNumber(userDetails);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount(userId)));
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable String notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
    }
}
