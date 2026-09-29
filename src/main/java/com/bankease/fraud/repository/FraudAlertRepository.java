package com.bankease.fraud.repository;

import com.bankease.fraud.document.FraudAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAlertRepository extends MongoRepository<FraudAlert, String> {
    Page<FraudAlert> findByAccountNumberOrderByCreatedAtDesc(String accountNumber, Pageable pageable);
    List<FraudAlert> findByAlertStatusOrderByCreatedAtDesc(String status);
    long countByAccountNumberAndAlertStatus(String accountNumber, String status);
}
