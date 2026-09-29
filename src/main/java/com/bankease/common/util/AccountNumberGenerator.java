package com.bankease.common.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String BANK_CODE = "BKE";

    private AccountNumberGenerator() {}

    /**
     * Generates a unique account number in the format: BKE-YYYYMMDD-XXXXXXXX
     * e.g., BKE20240101123456789
     */
    public static String generate() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long randomPart = (long) (RANDOM.nextDouble() * 9_000_000_000L) + 1_000_000_000L;
        return BANK_CODE + datePart + randomPart;
    }

    /**
     * Generates a 6-digit OTP
     */
    public static String generateOtp() {
        int otp = RANDOM.nextInt(900_000) + 100_000;
        return String.valueOf(otp);
    }

    /**
     * Generates a transaction reference number
     */
    public static String generateTransactionRef() {
        String prefix = "TXN";
        long timestamp = System.currentTimeMillis();
        int randomSuffix = RANDOM.nextInt(9000) + 1000;
        return prefix + timestamp + randomSuffix;
    }
}
