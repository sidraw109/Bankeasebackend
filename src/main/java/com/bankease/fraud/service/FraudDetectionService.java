package com.bankease.fraud.service;

import com.bankease.common.exception.FraudDetectedException;
import com.bankease.fraud.document.FraudAlert;
import com.bankease.fraud.repository.FraudAlertRepository;
import com.bankease.transaction.entity.Transaction;
import com.bankease.transaction.event.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based anomaly detection and risk scoring engine.
 * Analyzes each transaction in real-time.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final FraudAlertRepository fraudAlertRepository;

    // Thresholds
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("500000");
    private static final BigDecimal SUSPICIOUS_ROUND_NUMBER = new BigDecimal("100000");
    private static final int MAX_OPEN_ALERTS = 3;

    /**
     * Synchronous pre-transaction fraud check.
     * Called BEFORE a transaction is executed.
     * Throws FraudDetectedException if transaction must be blocked.
     */
    public RiskAssessment assessRisk(String accountNumber, BigDecimal amount, String transactionRef) {
        List<String> triggeredRules = new ArrayList<>();
        double riskScore = 0.0;

        // Rule 1: High value transaction
        if (amount.compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            triggeredRules.add("HIGH_VALUE_TRANSACTION");
            riskScore += 0.4;
        }

        // Rule 2: Round-number transactions (potential structuring)
        if (amount.remainder(SUSPICIOUS_ROUND_NUMBER).compareTo(BigDecimal.ZERO) == 0
                && amount.compareTo(SUSPICIOUS_ROUND_NUMBER) >= 0) {
            triggeredRules.add("ROUND_NUMBER_TRANSACTION");
            riskScore += 0.2;
        }

        // Rule 3: Account has many open fraud alerts
        long openAlerts = fraudAlertRepository.countByAccountNumberAndAlertStatus(accountNumber, "OPEN");
        if (openAlerts >= MAX_OPEN_ALERTS) {
            triggeredRules.add("MULTIPLE_OPEN_ALERTS");
            riskScore += 0.6;
        }

        // Normalize score
        riskScore = Math.min(riskScore, 1.0);

        FraudAlert.RiskLevel riskLevel = determineRiskLevel(riskScore);
        log.info("Risk assessment for {}: score={}, level={}, rules={}",
                accountNumber, riskScore, riskLevel, triggeredRules);

        // CRITICAL risk: block the transaction
        if (riskLevel == FraudAlert.RiskLevel.CRITICAL) {
            saveAlert(accountNumber, transactionRef, riskScore, riskLevel, triggeredRules, null);
            throw new FraudDetectedException(
                    "Transaction blocked due to high fraud risk. Please contact support.");
        }

        return new RiskAssessment(riskScore, riskLevel, triggeredRules);
    }

    /**
     * Async post-transaction analysis for audit and alerts.
     */
    @Async
    @EventListener
    public void analyzeCompletedTransaction(TransactionCompletedEvent event) {
        Transaction txn = event.getTransaction();
        log.info("[ASYNC] Running post-transaction fraud analysis for: {}", txn.getReferenceNumber());

        List<String> triggeredRules = new ArrayList<>();
        double riskScore = 0.0;

        if (txn.getAmount().compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            triggeredRules.add("HIGH_VALUE_TRANSACTION");
            riskScore += 0.3;
        }

        if (!triggeredRules.isEmpty()) {
            FraudAlert.RiskLevel riskLevel = determineRiskLevel(riskScore);
            saveAlert(txn.getSenderAccountNumber(), txn.getReferenceNumber(),
                    riskScore, riskLevel, triggeredRules, txn.getAmount());
        }
    }

    private void saveAlert(String accountNumber, String txnRef, double riskScore,
                           FraudAlert.RiskLevel riskLevel, List<String> rules, BigDecimal amount) {
        FraudAlert alert = FraudAlert.builder()
                .accountNumber(accountNumber)
                .transactionReference(txnRef)
                .riskLevel(riskLevel)
                .riskScore(riskScore)
                .triggeredRules(rules)
                .transactionAmount(amount)
                .alertStatus("OPEN")
                .build();
        fraudAlertRepository.save(alert);
        log.warn("Fraud alert created for account: {} - Level: {}", accountNumber, riskLevel);
    }

    private FraudAlert.RiskLevel determineRiskLevel(double score) {
        if (score >= 0.8) return FraudAlert.RiskLevel.CRITICAL;
        if (score >= 0.6) return FraudAlert.RiskLevel.HIGH;
        if (score >= 0.4) return FraudAlert.RiskLevel.MEDIUM;
        return FraudAlert.RiskLevel.LOW;
    }

    public record RiskAssessment(double riskScore, FraudAlert.RiskLevel riskLevel, List<String> triggeredRules) {}
}
