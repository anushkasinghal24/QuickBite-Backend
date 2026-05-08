package com.quickbite.delivery_service.repository;

import com.quickbite.delivery_service.entity.DeliveryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryHistoryRepository extends JpaRepository<DeliveryHistory, Long> {

    List<DeliveryHistory> findByAgentIdOrderByDeliveredAtDesc(Integer agentId);

    boolean existsByAgentIdAndOrderId(Integer agentId, Integer orderId);
}
