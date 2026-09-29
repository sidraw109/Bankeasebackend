package com.bankease;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableJpaRepositories(basePackages = {
    "com.bankease.account.repository",
    "com.bankease.transaction.repository",
    "com.bankease.payment.repository",
    "com.bankease.fraud.repository"
})
@EnableMongoRepositories(basePackages = {
    "com.bankease.notification.repository",
    "com.bankease.fraud.repository"
})
public class BankEaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankEaseApplication.class, args);
    }
}
