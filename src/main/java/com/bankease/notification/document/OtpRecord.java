package com.bankease.notification.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "otp_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpRecord {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String phone;

    @Indexed
    private String email;

    private String otpCode;

    private OtpPurpose purpose;

    private boolean used;

    @Indexed(expireAfterSeconds = 600) // TTL: 10 minutes
    private LocalDateTime expiresAt;

    @CreatedDate
    private LocalDateTime createdAt;

    private int attemptCount;

    public enum OtpPurpose {
        LOGIN, TRANSACTION, FORGOT_PASSWORD, ACCOUNT_ACTIVATION, CHANGE_PIN
    }
}
