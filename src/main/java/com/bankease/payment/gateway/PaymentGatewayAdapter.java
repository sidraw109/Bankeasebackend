package com.bankease.payment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simulated third-party payment gateway adapter.
 * In production, replace with actual gateway SDK calls (e.g., Razorpay, PayU, NPCI).
 */
@Slf4j
@Component
public class PaymentGatewayAdapter {

    /**
     * Initiates an inter-bank transfer via the external gateway.
     * Simulates a 95% success rate for demonstration purposes.
     */
    public GatewayResponse initiateTransfer(GatewayTransferRequest request) {
        log.info("Initiating gateway transfer: {} -> {} Amount: {}",
                request.getSourceAccount(), request.getBeneficiaryAccount(), request.getAmount());

        // Simulate gateway call latency
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate 95% success
        if (Math.random() > 0.05) {
            String gatewayTxnId = "GTW-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 16);
            log.info("Gateway transfer SUCCESS: {}", gatewayTxnId);
            return GatewayResponse.builder()
                    .success(true)
                    .gatewayTransactionId(gatewayTxnId)
                    .message("Transfer initiated successfully via " + request.getTransferMode())
                    .build();
        } else {
            log.warn("Gateway transfer FAILED: Simulated gateway rejection");
            return GatewayResponse.builder()
                    .success(false)
                    .message("Gateway rejected the transfer. Please try again later.")
                    .build();
        }
    }

    public GatewayResponse verifyTransfer(String gatewayTransactionId) {
        log.info("Verifying gateway transfer: {}", gatewayTransactionId);
        return GatewayResponse.builder()
                .success(true)
                .gatewayTransactionId(gatewayTransactionId)
                .message("Transfer verified successfully")
                .build();
    }

    @lombok.Builder
    @lombok.Data
    public static class GatewayTransferRequest {
        private String sourceAccount;
        private String beneficiaryAccount;
        private String beneficiaryName;
        private String beneficiaryIfsc;
        private BigDecimal amount;
        private String currency;
        private String transferMode;
        private String description;
        private String referenceId;
    }

    @lombok.Builder
    @lombok.Data
    public static class GatewayResponse {
        private boolean success;
        private String gatewayTransactionId;
        private String message;
        private String errorCode;
    }
}
