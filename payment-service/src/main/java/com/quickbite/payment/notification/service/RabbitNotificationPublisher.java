package com.quickbite.payment.notification.service;

import com.quickbite.payment.notification.constants.AppConstants;
import com.quickbite.payment.notification.dto.request.SendNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitNotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public boolean publishEmail(SendNotificationRequest request) {
        return publish(AppConstants.RABBIT_NOTIFICATION_EMAIL_KEY, request, "EMAIL");
    }

    public boolean publishSms(SendNotificationRequest request) {
        return publish(AppConstants.RABBIT_NOTIFICATION_SMS_KEY, request, "SMS");
    }

    private boolean publish(String routingKey, SendNotificationRequest request, String channel) {
        try {
            rabbitTemplate.convertAndSend(
                    AppConstants.RABBIT_NOTIFICATION_EXCHANGE,
                    routingKey,
                    request);
            log.info("Notification queued via RabbitMQ: channel={}, type={}, recipientId={}",
                    channel, request.getType(), request.getRecipientId());
            return true;
        } catch (Exception ex) {
            log.error("RabbitMQ publish failed for channel={}, type={}, recipientId={}: {}",
                    channel, request.getType(), request.getRecipientId(), ex.getMessage());
            return false;
        }
    }
}
