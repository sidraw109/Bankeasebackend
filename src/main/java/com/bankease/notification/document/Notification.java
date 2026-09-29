package com.bankease.notification.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String accountNumber;

    private NotificationType type;

    private String title;

    private String message;

    private NotificationChannel channel; // SMS, EMAIL, PUSH

    private DeliveryStatus status;

    private String referenceId; // Transaction ref or order id

    private Map<String, Object> metadata;

    private boolean read;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime deliveredAt;

    public enum NotificationType {
        TRANSACTION_ALERT, OTP, ACCOUNT_ACTIVATION, KYC_STATUS, SECURITY_ALERT, RECEIPT, PROMOTIONAL
    }

    public enum NotificationChannel {
        SMS, EMAIL, PUSH, IN_APP
    }

    public enum DeliveryStatus {
        PENDING, SENT, DELIVERED, FAILED
    }
}
