package com.bankease.transaction.event;

import com.bankease.transaction.entity.Transaction;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TransactionFailedEvent extends ApplicationEvent {
    private final Transaction transaction;
    private final String reason;

    public TransactionFailedEvent(Object source, Transaction transaction, String reason) {
        super(source);
        this.transaction = transaction;
        this.reason = reason;
    }
}
