package com.bankease.fraud.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "fraud_alerts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlert {

    @Id
    private String id;

    @Indexed
    private String accountNumber;

    @Indexed
    private String transactionReference;

    private RiskLevel riskLevel;

    private double riskScore; // 0.0 - 1.0

    private List<String> triggeredRules;

    private BigDecimal transactionAmount;

    private String alertStatus; // OPEN, INVESTIGATING, RESOLVED, FALSE_POSITIVE

    private String resolvedBy;

    private String resolution;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
