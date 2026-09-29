package com.bankease.account.event;

import com.bankease.account.entity.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class KycSubmittedEvent extends ApplicationEvent {
    private final User user;

    public KycSubmittedEvent(Object source, User user) {
        super(source);
        this.user = user;
    }
}
