package com.bankease.notification.repository;

import com.bankease.notification.document.OtpRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends MongoRepository<OtpRecord, String> {

    Optional<OtpRecord> findTopByPhoneAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String phone, OtpRecord.OtpPurpose purpose, LocalDateTime now);

    Optional<OtpRecord> findTopByEmailAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, OtpRecord.OtpPurpose purpose, LocalDateTime now);

    long countByPhoneAndCreatedAtAfter(String phone, LocalDateTime since);
}
