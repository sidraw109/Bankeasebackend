package com.bankease.payment.repository;

import com.bankease.payment.entity.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderId(String orderId);
    Optional<PaymentOrder> findByIdempotencyKey(String idempotencyKey);
    Page<PaymentOrder> findByAccountNumberOrderByCreatedAtDesc(String accountNumber, Pageable pageable);
    boolean existsByIdempotencyKey(String idempotencyKey);
}
