package com.quickbite.restaurant.repository;

import com.quickbite.restaurant.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RestaurantRepository
 *
 * PDF Section 4.2 ke sabhi methods:
 * findByOwnerId, findByCuisine, findByCity,
 * findByIsOpenAndIsApproved, searchByName, findNearby, countByCity
 *
 * Plus additional helpers for admin panel and analytics.
 */
@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    // ===== OWNER QUERIES =====
    List<Restaurant> findByOwnerIdAndIsActiveTrue(Long ownerId);
    Optional<Restaurant> findByRestaurantIdAndOwnerId(Long restaurantId, Long ownerId);

    // ===== DISCOVERY (customer-facing, only approved restaurants) =====
    Page<Restaurant> findByApprovalStatusAndIsOpenAndIsActiveTrue(
            String approvalStatus, Boolean isOpen, Pageable pageable);

    Page<Restaurant> findByCuisineIgnoreCaseAndApprovalStatusAndIsActiveTrue(
            String cuisine, String approvalStatus, Pageable pageable);

    Page<Restaurant> findByCityIgnoreCaseAndApprovalStatusAndIsActiveTrue(
            String city, String approvalStatus, Pageable pageable);

    // ===== SEARCH by name (LIKE) =====
    @Query("SELECT r FROM Restaurant r WHERE " +
           "LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND r.approvalStatus = 'APPROVED' AND r.isActive = true")
    Page<Restaurant> searchByName(@Param("keyword") String keyword, Pageable pageable);

    // ===== ADVANCED SEARCH (name OR cuisine OR city) =====
    @Query("SELECT r FROM Restaurant r WHERE " +
           "(LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(r.cuisine) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(r.city) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND r.approvalStatus = 'APPROVED' AND r.isActive = true")
    Page<Restaurant> globalSearch(@Param("keyword") String keyword, Pageable pageable);

    /**
     * GEO-PROXIMITY SEARCH using Haversine formula (PDF Section 6 — NFR).
     * Finds all approved, open restaurants within :radiusKm kilometres.
     *
     * Formula explanation:
     *   d = 2R * arcsin(sqrt(
     *         sin^2((lat2-lat1)/2) + cos(lat1)*cos(lat2)*sin^2((lon2-lon1)/2)
     *       ))
     *
     * MySQL radians() and 6371 (earth radius km) used directly.
     */
    @Query(value = """
            SELECT r.* FROM restaurants r
            WHERE r.approval_status = 'APPROVED'
              AND r.is_open = true
              AND r.is_active = true
              AND (6371 * ACOS(
                    COS(RADIANS(:lat)) * COS(RADIANS(r.latitude))
                    * COS(RADIANS(r.longitude) - RADIANS(:lng))
                    + SIN(RADIANS(:lat)) * SIN(RADIANS(r.latitude))
                  )) <= :radiusKm
            ORDER BY r.avg_rating DESC
            """,
            nativeQuery = true)
    List<Restaurant> findNearby(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radiusKm") Double radiusKm);

    // ===== RATING FILTER =====
    @Query("SELECT r FROM Restaurant r WHERE r.approvalStatus = 'APPROVED' " +
           "AND r.isActive = true AND r.avgRating >= :minRating")
    Page<Restaurant> findByMinRating(@Param("minRating") Double minRating, Pageable pageable);

    // ===== ADMIN QUERIES =====
    Page<Restaurant> findByApprovalStatus(String approvalStatus, Pageable pageable);
    List<Restaurant> findByApprovalStatus(String approvalStatus);
    Page<Restaurant> findByApprovalStatusAndIsActiveTrue(String approvalStatus, Pageable pageable);
    long countByApprovalStatus(String approvalStatus);
    long countByCityIgnoreCase(String city);

    // ===== UPDATE RATING (called by review-service via Feign) =====
    @Modifying
    @Query("UPDATE Restaurant r SET r.avgRating = :rating, r.totalReviews = :totalReviews " +
           "WHERE r.restaurantId = :restaurantId")
    void updateRating(@Param("restaurantId") Long restaurantId,
                      @Param("rating") Double rating,
                      @Param("totalReviews") Integer totalReviews);

    // ===== APPROVAL =====
    @Modifying
    @Query("UPDATE Restaurant r SET r.approvalStatus = :status, " +
           "r.rejectionReason = :reason WHERE r.restaurantId = :id")
    void updateApprovalStatus(@Param("id") Long id,
                              @Param("status") String status,
                              @Param("reason") String reason);

    // ===== TOGGLE OPEN =====
    @Modifying
    @Query("UPDATE Restaurant r SET r.isOpen = :isOpen WHERE r.restaurantId = :id")
    void updateIsOpen(@Param("id") Long id, @Param("isOpen") Boolean isOpen);

    // ===== EXISTS CHECK =====
    boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name);
    boolean existsByPhone(String phone);
}
