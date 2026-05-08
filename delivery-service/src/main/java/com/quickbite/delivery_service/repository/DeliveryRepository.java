package com.quickbite.delivery_service.repository;

import com.quickbite.delivery_service.entity.DeliveryAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DeliveryRepository
 *
 * PDF Section 4.7 exact methods:
 *  findByUserId(), findByAgentId(), findByIsAvailable(), findByIsVerified(),
 *  findNearbyAgents(), countByIsAvailable(), deleteByAgentId()
 */
@Repository
public interface DeliveryRepository extends JpaRepository<DeliveryAgent, Integer> {

    // ── PDF Methods ───────────────────────────────────────────────────────────

    Optional<DeliveryAgent> findByUserId(Integer userId);

    Optional<DeliveryAgent> findByAgentId(Integer agentId);

    List<DeliveryAgent> findByIsAvailable(Boolean isAvailable);

    List<DeliveryAgent> findByIsVerified(Boolean isVerified);

    long countByIsAvailable(Boolean isAvailable);

    void deleteByAgentId(Integer agentId);

    // ── Status-based queries ──────────────────────────────────────────────────

    List<DeliveryAgent> findByStatus(DeliveryAgent.AgentStatus status);

    long countByStatus(DeliveryAgent.AgentStatus status);

    // ── Order assignment ──────────────────────────────────────────────────────

    /** Find agents with no current order assigned */
    List<DeliveryAgent> findByCurrentOrderIdIsNull();

    Optional<DeliveryAgent> findByCurrentOrderId(Integer orderId);

    // ── Haversine-based geo-proximity query ───────────────────────────────────
    /**
     * PDF Section 4.7: findNearbyAgents()
     * PDF Section 6 NFR: "Nearby restaurant and agent discovery uses the Haversine formula"
     *
     * Finds available + verified agents within `radiusKm` of the given coordinates.
     * Used by order-service to assign nearest free agent.
     *
     * Haversine formula in SQL:
     *  distance = 6371 * acos(cos(lat1) * cos(lat2) * cos(lon2-lon1) + sin(lat1) * sin(lat2))
     */
    @Query(value = """
        SELECT * FROM delivery_agents da
        WHERE da.is_available = true
          AND da.is_verified = true
          AND da.status = 'VERIFIED'
          AND da.current_order_id IS NULL
          AND da.current_latitude IS NOT NULL
          AND da.current_longitude IS NOT NULL
          AND (
            6371 * acos(
              cos(radians(:lat)) * cos(radians(da.current_latitude))
              * cos(radians(da.current_longitude) - radians(:lng))
              + sin(radians(:lat)) * sin(radians(da.current_latitude))
            )
          ) <= :radiusKm
        ORDER BY (
            6371 * acos(
              cos(radians(:lat)) * cos(radians(da.current_latitude))
              * cos(radians(da.current_longitude) - radians(:lng))
              + sin(radians(:lat)) * sin(radians(da.current_latitude))
            )
          ) ASC
        LIMIT 10
        """, nativeQuery = true)
    List<DeliveryAgent> findNearbyAgents(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radiusKm") Double radiusKm
    );

    // ── Analytics ─────────────────────────────────────────────────────────────

    boolean existsByUserId(Integer userId);

    /** Top agents by rating */
    List<DeliveryAgent> findTop10ByIsVerifiedTrueOrderByAvgRatingDesc();

    /** Agents with at least one delivery */
    List<DeliveryAgent> findByTotalDeliveriesGreaterThan(Integer count);
}
