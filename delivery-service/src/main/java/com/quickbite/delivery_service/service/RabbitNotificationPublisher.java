package com.quickbite.delivery_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitNotificationPublisher {

    private static final String EXCHANGE = "quickbite.payment.notification.exchange";
    private static final String ROUTING_KEY = "quickbite.payment.notification";

    private final RabbitTemplate rabbitTemplate;

    public boolean publish(Map<String, Object> payload) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, payload);
            log.info("Delivery notification queued via RabbitMQ: type={}, recipientId={}",
                    payload.get("type"), payload.get("recipientId"));
            return true;
        } catch (Exception ex) {
            log.error("RabbitMQ publish failed for delivery notification type={}, recipientId={}: {}",
                    payload.get("type"), payload.get("recipientId"), ex.getMessage());
            return false;
        }
    }
}
