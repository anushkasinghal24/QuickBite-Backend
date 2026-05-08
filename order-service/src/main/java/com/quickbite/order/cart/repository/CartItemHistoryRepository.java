package com.quickbite.order.cart.repository;

import com.quickbite.order.cart.entity.CartItemHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemHistoryRepository extends JpaRepository<CartItemHistory, Long> {

    List<CartItemHistory> findByOrderIdOrderByArchivedAtDesc(Integer orderId);

    List<CartItemHistory> findByCustomerIdOrderByArchivedAtDesc(Integer customerId);
}
