package com.bankease.notification.service;

import com.bankease.common.exception.BusinessException;
import com.bankease.common.util.AccountNumberGenerator;
import com.bankease.notification.document.OtpRecord;
import com.bankease.notification.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private static final int MAX_OTP_PER_HOUR = 5;

    public String generateAndSendOtp(String identifier, OtpRecord.OtpPurpose purpose, boolean isEmail) {
        // Rate limit: max 5 OTPs per hour
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentOtps = otpRepository.countByPhoneAndCreatedAtAfter(identifier, oneHourAgo);
        if (recentOtps >= MAX_OTP_PER_HOUR) {
            throw new BusinessException("OTP limit exceeded. Please try after 1 hour.", "OTP_LIMIT_EXCEEDED");
        }

        String otpCode = AccountNumberGenerator.generateOtp();

        OtpRecord otpRecord = OtpRecord.builder()
                .otpCode(otpCode)
                .purpose(purpose)
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .attemptCount(0)
                .build();

        if (isEmail) {
            otpRecord.setEmail(identifier);
        } else {
            otpRecord.setPhone(identifier);
        }

        otpRepository.save(otpRecord);

        // In production: integrate with SMS/Email provider (Twilio, SendGrid, AWS SES)
        log.info("OTP generated for {}: {} (Purpose: {})", identifier, otpCode, purpose);

        // Return masked OTP (for testing; in prod, only send via SMS/Email)
        return "OTP sent to " + maskIdentifier(identifier, isEmail);
    }

    public boolean verifyOtp(String identifier, String otpCode, OtpRecord.OtpPurpose purpose, boolean isEmail) {
        LocalDateTime now = LocalDateTime.now();

        OtpRecord otpRecord;
        if (isEmail) {
            otpRecord = otpRepository
                    .findTopByEmailAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                            identifier, purpose, now)
                    .orElseThrow(() -> new BusinessException("OTP not found or expired", "OTP_INVALID"));
        } else {
            otpRecord = otpRepository
                    .findTopByPhoneAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                            identifier, purpose, now)
                    .orElseThrow(() -> new BusinessException("OTP not found or expired", "OTP_INVALID"));
        }

        otpRecord.setAttemptCount(otpRecord.getAttemptCount() + 1);

        if (otpRecord.getAttemptCount() > 3) {
            otpRecord.setUsed(true);
            otpRepository.save(otpRecord);
            throw new BusinessException("Maximum OTP attempts exceeded. Please request a new OTP.", "OTP_MAX_ATTEMPTS");
        }

        if (!otpRecord.getOtpCode().equals(otpCode)) {
            otpRepository.save(otpRecord);
            throw new BusinessException(
                    "Invalid OTP. " + (3 - otpRecord.getAttemptCount()) + " attempts remaining.",
                    "OTP_MISMATCH");
        }

        otpRecord.setUsed(true);
        otpRepository.save(otpRecord);
        log.info("OTP verified successfully for: {}", identifier);
        return true;
    }

    private String maskIdentifier(String identifier, boolean isEmail) {
        if (isEmail) {
            int atIndex = identifier.indexOf('@');
            return identifier.substring(0, Math.min(3, atIndex)) + "***" + identifier.substring(atIndex);
        } else {
            return "******" + identifier.substring(Math.max(0, identifier.length() - 4));
        }
    }
}
