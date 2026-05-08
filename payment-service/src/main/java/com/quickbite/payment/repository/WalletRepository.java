package com.quickbite.payment.repository;

import com.quickbite.payment.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByCustomerId(Long customerId);

    boolean existsByCustomerId(Long customerId);

    @Modifying
    @Query("UPDATE Wallet w SET w.balance = w.balance + :amount WHERE w.customerId = :customerId")
    void creditBalance(@Param("customerId") Long customerId, @Param("amount") Double amount);

    @Modifying
    @Query("UPDATE Wallet w SET w.balance = w.balance - :amount WHERE w.customerId = :customerId")
    void debitBalance(@Param("customerId") Long customerId, @Param("amount") Double amount);
}
