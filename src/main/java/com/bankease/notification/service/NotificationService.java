package com.bankease.notification.service;

import com.bankease.notification.document.Notification;
import com.bankease.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification sendNotification(String userId, String accountNumber, String title,
                                         String message, Notification.NotificationType type,
                                         Notification.NotificationChannel channel,
                                         String referenceId, Map<String, Object> metadata) {
        Notification notification = Notification.builder()
                .userId(userId)
                .accountNumber(accountNumber)
                .type(type)
                .title(title)
                .message(message)
                .channel(channel)
                .status(Notification.DeliveryStatus.PENDING)
                .referenceId(referenceId)
                .metadata(metadata)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);

        // Simulate delivery (in prod: SMS via Twilio, Email via SendGrid, Push via FCM)
        notification.setStatus(Notification.DeliveryStatus.SENT);
        notification.setDeliveredAt(LocalDateTime.now());
        notificationRepository.save(notification);

        log.info("Notification sent: type={}, userId={}, channel={}", type, userId, channel);
        return notification;
    }

    public Page<Notification> getUserNotifications(String userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    public Page<Notification> getUnreadNotifications(String userId, int page, int size) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}
