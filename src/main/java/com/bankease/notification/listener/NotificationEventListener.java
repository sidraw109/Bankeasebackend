package com.bankease.notification.listener;

import com.bankease.account.entity.User;
import com.bankease.account.event.KycApprovedEvent;
import com.bankease.account.event.KycSubmittedEvent;
import com.bankease.account.event.UserRegisteredEvent;
import com.bankease.notification.document.Notification;
import com.bankease.notification.service.NotificationService;
import com.bankease.transaction.entity.Transaction;
import com.bankease.transaction.event.TransactionCompletedEvent;
import com.bankease.transaction.event.TransactionFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Async event listeners for the Notification module.
 * All methods annotated with @Async to run in a separate thread pool.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        User user = event.getUser();
        log.info("[ASYNC] Processing registration notification for user: {}", user.getEmail());

        notificationService.sendNotification(
                user.getAccountNumber(),
                user.getAccountNumber(),
                "Welcome to BankEase!",
                String.format("Dear %s, your account has been created successfully. Account Number: %s. " +
                        "Please complete KYC to activate your account.", user.getFirstName(), user.getAccountNumber()),
                Notification.NotificationType.ACCOUNT_ACTIVATION,
                Notification.NotificationChannel.EMAIL,
                user.getAccountNumber(),
                Map.of("accountNumber", user.getAccountNumber(), "firstName", user.getFirstName())
        );
    }

    @Async
    @EventListener
    public void onKycSubmitted(KycSubmittedEvent event) {
        User user = event.getUser();
        log.info("[ASYNC] Processing KYC submitted notification for user: {}", user.getId());

        notificationService.sendNotification(
                user.getAccountNumber(),
                user.getAccountNumber(),
                "KYC Submitted Successfully",
                String.format("Dear %s, your KYC documents have been received and are under review. " +
                        "We will notify you once verification is complete.", user.getFirstName()),
                Notification.NotificationType.KYC_STATUS,
                Notification.NotificationChannel.EMAIL,
                null,
                Map.of("status", "SUBMITTED")
        );
    }

    @Async
    @EventListener
    public void onKycApproved(KycApprovedEvent event) {
        User user = event.getUser();
        log.info("[ASYNC] Processing KYC approved notification for user: {}", user.getId());

        notificationService.sendNotification(
                user.getAccountNumber(),
                user.getAccountNumber(),
                "KYC Approved - Account Activated!",
                String.format("Congratulations %s! Your KYC has been approved and your account is now active. " +
                        "You can now start transacting.", user.getFirstName()),
                Notification.NotificationType.KYC_STATUS,
                Notification.NotificationChannel.SMS,
                null,
                Map.of("status", "APPROVED")
        );
    }

    @Async
    @EventListener
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        Transaction txn = event.getTransaction();
        log.info("[ASYNC] Processing transaction alert for ref: {}", txn.getReferenceNumber());

        // Sender alert
        Map<String, Object> senderMeta = new HashMap<>();
        senderMeta.put("referenceNumber", txn.getReferenceNumber());
        senderMeta.put("amount", txn.getAmount().toString());
        senderMeta.put("receiver", txn.getReceiverAccountNumber());
        senderMeta.put("type", "DEBIT");

        notificationService.sendNotification(
                txn.getSenderAccountNumber(),
                txn.getSenderAccountNumber(),
                "Money Transferred Successfully",
                String.format("₹%.2f debited from account %s. Transferred to %s. Ref: %s.",
                        txn.getAmount(), txn.getSenderAccountNumber(),
                        txn.getReceiverAccountNumber(), txn.getReferenceNumber()),
                Notification.NotificationType.TRANSACTION_ALERT,
                Notification.NotificationChannel.SMS,
                txn.getReferenceNumber(),
                senderMeta
        );

        // Receiver alert
        Map<String, Object> receiverMeta = new HashMap<>();
        receiverMeta.put("referenceNumber", txn.getReferenceNumber());
        receiverMeta.put("amount", txn.getAmount().toString());
        receiverMeta.put("sender", txn.getSenderAccountNumber());
        receiverMeta.put("type", "CREDIT");

        notificationService.sendNotification(
                txn.getReceiverAccountNumber(),
                txn.getReceiverAccountNumber(),
                "Money Received",
                String.format("₹%.2f credited to account %s from %s. Ref: %s.",
                        txn.getAmount(), txn.getReceiverAccountNumber(),
                        txn.getSenderAccountNumber(), txn.getReferenceNumber()),
                Notification.NotificationType.TRANSACTION_ALERT,
                Notification.NotificationChannel.SMS,
                txn.getReferenceNumber(),
                receiverMeta
        );
    }

    @Async
    @EventListener
    public void onTransactionFailed(TransactionFailedEvent event) {
        Transaction txn = event.getTransaction();
        log.info("[ASYNC] Processing transaction failure notification for ref: {}", txn.getReferenceNumber());

        notificationService.sendNotification(
                txn.getSenderAccountNumber(),
                txn.getSenderAccountNumber(),
                "Transaction Failed",
                String.format("Transaction of ₹%.2f failed. Reason: %s. Ref: %s. No amount was deducted.",
                        txn.getAmount(), event.getReason(), txn.getReferenceNumber()),
                Notification.NotificationType.TRANSACTION_ALERT,
                Notification.NotificationChannel.SMS,
                txn.getReferenceNumber(),
                Map.of("reason", event.getReason(), "status", "FAILED")
        );
    }
}
