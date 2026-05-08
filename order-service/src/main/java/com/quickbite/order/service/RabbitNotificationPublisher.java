package com.quickbite.order.service;

import com.quickbite.order.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitNotificationPublisher {

    private static final String EXCHANGE = "quickbite.payment.notification.exchange";
    private static final String ROUTING_KEY = "quickbite.payment.notification";

    private final RabbitTemplate rabbitTemplate;

    public boolean publish(NotificationRequest request) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, request);
            log.info("Order notification queued via RabbitMQ: type={}, recipientId={}",
                    request.getType(), request.getRecipientId());
            return true;
        } catch (Exception ex) {
            log.error("RabbitMQ publish failed for order notification type={}, recipientId={}: {}",
                    request.getType(), request.getRecipientId(), ex.getMessage());
            return false;
        }
    }
}
