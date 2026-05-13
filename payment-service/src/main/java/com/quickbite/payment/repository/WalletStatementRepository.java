package com.quickbite.payment.repository;

import com.quickbite.payment.entity.WalletStatement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletStatementRepository extends JpaRepository<WalletStatement, Long> {

    List<WalletStatement> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Page<WalletStatement> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    List<WalletStatement> findByCustomerIdAndTypeOrderByCreatedAtDesc(Long customerId, String type);
}
