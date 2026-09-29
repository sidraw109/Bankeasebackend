package com.bankease.transaction.event;

import com.bankease.transaction.entity.Transaction;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TransactionCompletedEvent extends ApplicationEvent {
    private final Transaction transaction;

    public TransactionCompletedEvent(Object source, Transaction transaction) {
        super(source);
        this.transaction = transaction;
    }
}
