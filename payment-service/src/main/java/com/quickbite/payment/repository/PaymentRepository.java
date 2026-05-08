package com.quickbite.payment.repository;

import com.quickbite.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * PaymentRepository â€” PDF Section 4.6
 * findByOrderId, findByCustomerId, findByStatus, findByTransactionId,
 * findByPaidAtBetween, sumAmountByCustomerId
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Page<Payment> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    List<Payment> findByStatus(String status);

    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByPaidAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.customerId = :customerId AND p.status = 'PAID'")
    Double sumPaidAmountByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.status = 'PAID' AND p.paidAt BETWEEN :start AND :end")
    Double sumRevenueBetween(@Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);

    @Modifying
    @Query("UPDATE Payment p SET p.status = :status, p.refundedAt = :refundedAt, " +
           "p.refundTransactionId = :refundTxn WHERE p.paymentId = :id")
    void updateToRefunded(@Param("id") Long id,
                          @Param("status") String status,
                          @Param("refundedAt") LocalDateTime refundedAt,
                          @Param("refundTxn") String refundTxn);

    @Modifying
    @Query("UPDATE Payment p SET p.status = :status, p.paidAt = :paidAt " +
           "WHERE p.paymentId = :id")
    void updateStatusAndPaidAt(@Param("id") Long id,
                               @Param("status") String status,
                               @Param("paidAt") LocalDateTime paidAt);

    long countByStatus(String status);

    boolean existsByOrderId(Long orderId);
}
