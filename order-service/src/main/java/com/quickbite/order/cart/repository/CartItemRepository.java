package com.quickbite.order.cart.repository;

import  com.quickbite.order.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    /** Get all items for a specific cart */
    List<CartItem> findByCartCartId(int cartId);

    /** Find a specific item in a cart */
    Optional<CartItem> findByCartCartIdAndMenuItemId(int cartId, int menuItemId);

    /** Delete all items of a cart */
    void deleteByCartCartId(int cartId);
}
