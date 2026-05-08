package com.quickbite.restaurant.repository;

import com.quickbite.restaurant.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    // ===== BY RESTAURANT =====
    List<MenuItem> findByRestaurantIdAndIsActiveTrueOrderByMenuCategory_DisplayOrderAsc(
            Long restaurantId);

    // ===== BY CATEGORY =====
    List<MenuItem> findByMenuCategory_CategoryIdAndIsActiveTrue(Long categoryId);

    // ===== VEG FILTER =====
    List<MenuItem> findByRestaurantIdAndIsVegAndIsActiveTrue(Long restaurantId, Boolean isVeg);

    // ===== AVAILABILITY FILTER =====
    List<MenuItem> findByRestaurantIdAndIsAvailableAndIsActiveTrue(
            Long restaurantId, Boolean isAvailable);

    // ===== SEARCH by name within restaurant =====
    @Query("SELECT i FROM MenuItem i WHERE i.restaurantId = :restaurantId " +
           "AND LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND i.isActive = true")
    List<MenuItem> searchByName(@Param("restaurantId") Long restaurantId,
                                @Param("keyword") String keyword);

    // ===== PRICE FILTER =====
    List<MenuItem> findByRestaurantIdAndPriceLessThanEqualAndIsActiveTrue(
            Long restaurantId, Double maxPrice);

    // ===== COUNT =====
    long countByRestaurantIdAndIsActiveTrue(Long restaurantId);

    // ===== TOGGLE AVAILABILITY =====
    @Modifying
    @Query("UPDATE MenuItem i SET i.isAvailable = :available WHERE i.itemId = :itemId")
    void updateAvailability(@Param("itemId") Long itemId, @Param("available") Boolean available);

    // ===== GET BY ITEM ID for cart-service validation =====
    Optional<MenuItem> findByItemIdAndIsActiveTrue(Long itemId);

    // ===== CHECK EXISTS =====
    boolean existsByNameIgnoreCaseAndMenuCategory_CategoryId(String name, Long categoryId);

    // ===== DEACTIVATE ALL on restaurant delete =====
    @Modifying
    @Query("UPDATE MenuItem i SET i.isActive = false WHERE i.restaurantId = :restaurantId")
    void deactivateAllByRestaurantId(@Param("restaurantId") Long restaurantId);
}
