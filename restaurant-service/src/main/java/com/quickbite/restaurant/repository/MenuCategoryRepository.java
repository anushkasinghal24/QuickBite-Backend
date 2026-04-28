package com.quickbite.restaurant.repository;

import com.quickbite.restaurant.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    List<MenuCategory> findByRestaurant_RestaurantIdAndIsActiveTrueOrderByDisplayOrderAsc(
            Long restaurantId);

    Optional<MenuCategory> findByCategoryIdAndRestaurant_RestaurantId(
            Long categoryId, Long restaurantId);

    boolean existsByNameIgnoreCaseAndRestaurant_RestaurantId(String name, Long restaurantId);

    long countByRestaurant_RestaurantId(Long restaurantId);

    @Modifying
    @Query("UPDATE MenuCategory c SET c.isActive = false WHERE c.restaurant.restaurantId = :restaurantId")
    void deactivateAllByRestaurantId(@Param("restaurantId") Long restaurantId);
}
