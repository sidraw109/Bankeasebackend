package com.bankease.fraud.repository;

import com.bankease.fraud.entity.DisputeTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DisputeTicketRepository extends JpaRepository<DisputeTicket, Long> {
    Optional<DisputeTicket> findByTicketNumber(String ticketNumber);
    Page<DisputeTicket> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<DisputeTicket> findByStatusOrderByCreatedAtDesc(DisputeTicket.TicketStatus status, Pageable pageable);
    long countByTicketNumberStartingWith(String prefix);
}
