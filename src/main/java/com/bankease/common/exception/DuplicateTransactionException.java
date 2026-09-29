package com.bankease.common.exception;

public class DuplicateTransactionException extends RuntimeException {
    public DuplicateTransactionException(String idempotencyKey) {
        super("Transaction with idempotency key already exists: " + idempotencyKey);
    }
}
