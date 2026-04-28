package com.quickbite.order.cart.repository;

import com.quickbite.order.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CartRepository
 *
 * As per PDF (Section 4.4):
 *  findByCustomerId(), findByCartId(), existsByCustomerId(),
 *  findByRestaurantId(), deleteByCustomerId()
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {

    /** Get the single active cart for a customer */
    Optional<Cart> findByCustomerId(int customerId);

    /** Check if a cart already exists for customer */
    boolean existsByCustomerId(int customerId);

    /** Find all carts for a given restaurant (admin use) */
    List<Cart> findByRestaurantId(int restaurantId);

    /** Delete customer's cart (used after order placement) */
    void deleteByCustomerId(int customerId);
}
