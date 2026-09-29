package com.bankease.transaction.repository;

import com.bankease.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    Page<Transaction> findBySenderAccountNumberOrReceiverAccountNumberOrderByCreatedAtDesc(
            String sender, String receiver, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE " +
           "(t.senderAccountNumber = :accountNumber OR t.receiverAccountNumber = :accountNumber) " +
           "AND t.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountNumberAndDateRange(
            @Param("accountNumber") String accountNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE " +
           "t.senderAccountNumber = :accountNumber AND t.status = 'SUCCESS' " +
           "AND t.createdAt >= :since")
    BigDecimal sumSentAmountSince(
            @Param("accountNumber") String accountNumber,
            @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE " +
           "t.senderAccountNumber = :accountNumber AND t.createdAt >= :since")
    long countTransactionsSince(
            @Param("accountNumber") String accountNumber,
            @Param("since") LocalDateTime since);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
