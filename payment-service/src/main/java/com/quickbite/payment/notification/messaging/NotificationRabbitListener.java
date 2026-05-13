package com.quickbite.payment.notification.messaging;

import com.quickbite.payment.notification.constants.AppConstants;
import com.quickbite.payment.notification.dto.request.SendNotificationRequest;
import com.quickbite.payment.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRabbitListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = AppConstants.RABBIT_NOTIFICATION_QUEUE)
    public void handleNotification(SendNotificationRequest request) {
        if (request == null) {
            log.warn("Skipping null notification from RabbitMQ");
            return;
        }
        notificationService.send(request);
    }

    @RabbitListener(queues = AppConstants.RABBIT_NOTIFICATION_EMAIL_QUEUE)
    public void handleEmailNotification(SendNotificationRequest request) {
        if (request == null || request.getRecipientEmail() == null || request.getRecipientEmail().isBlank()) {
            log.warn("Skipping EMAIL notification from RabbitMQ because recipientEmail is missing");
            return;
        }
        notificationService.sendEmail(request.getRecipientEmail(), request.getTitle(), request.getMessage());
    }

    @RabbitListener(queues = AppConstants.RABBIT_NOTIFICATION_SMS_QUEUE)
    public void handleSmsNotification(SendNotificationRequest request) {
        if (request == null || request.getRecipientPhone() == null || request.getRecipientPhone().isBlank()) {
            log.warn("Skipping SMS notification from RabbitMQ because recipientPhone is missing");
            return;
        }
        notificationService.sendSms(request.getRecipientPhone(), request.getMessage());
    }
}
