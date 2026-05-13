package com.quickbite.payment.notification.service.impl;

import com.quickbite.payment.dto.response.PagedResponse;
import com.quickbite.payment.notification.constants.AppConstants;
import com.quickbite.payment.notification.dto.request.BulkNotificationRequest;
import com.quickbite.payment.notification.dto.request.SendNotificationRequest;
import com.quickbite.payment.notification.dto.response.NotificationResponse;
import com.quickbite.payment.notification.entity.Notification;
import com.quickbite.payment.notification.repository.NotificationRepository;
import com.quickbite.payment.notification.service.EmailService;
import com.quickbite.payment.notification.service.NotificationService;
import com.quickbite.payment.notification.service.RabbitNotificationPublisher;
import com.quickbite.payment.notification.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final RabbitNotificationPublisher rabbitNotificationPublisher;

    @Override
    public NotificationResponse send(SendNotificationRequest request) {
        log.info("Sending notification: type={}, channel={}, recipientId={}",
                request.getType(), request.getChannel(), request.getRecipientId());

        NotificationResponse savedResponse = null;
        String channel = request.getChannel() != null
                ? request.getChannel().toUpperCase() : AppConstants.CHANNEL_APP;
        boolean audible = Boolean.TRUE.equals(request.getAudible())
                || AppConstants.NEW_ORDER_ALERT.equals(request.getType());

        if (AppConstants.CHANNEL_APP.equals(channel) || AppConstants.CHANNEL_ALL.equals(channel)) {
            Notification notification = Notification.builder()
                    .recipientId(request.getRecipientId())
                    .type(request.getType())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .channel(AppConstants.CHANNEL_APP)
                    .relatedId(request.getRelatedId())
                    .relatedType(request.getRelatedType())
                    .deepLinkUrl(request.getDeepLinkUrl())
                    .isRead(false)
                    .audible(audible)
                    .build();
            savedResponse = mapToResponse(notificationRepository.save(notification));
            log.debug("APP notification persisted: id={}", savedResponse.getNotificationId());
        }

        if ((AppConstants.CHANNEL_EMAIL.equals(channel) || AppConstants.CHANNEL_ALL.equals(channel))
                && request.getRecipientEmail() != null) {
            dispatchEmail(request);
        }

        if ((AppConstants.CHANNEL_SMS.equals(channel) || AppConstants.CHANNEL_ALL.equals(channel))
                && request.getRecipientPhone() != null) {
            dispatchSms(request);
        }

        if (savedResponse == null) {
            savedResponse = NotificationResponse.builder()
                    .recipientId(request.getRecipientId())
                    .type(request.getType())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .channel(channel)
                    .isRead(false)
                    .audible(audible)
                    .sentAt(LocalDateTime.now())
                    .build();
        }

        return savedResponse;
    }

    @Override
    public int sendBulk(BulkNotificationRequest request) {
        log.info("Bulk notification: type={}, channel={}, recipients={}",
                request.getType(), request.getChannel(),
                request.getRecipientIds() != null ? request.getRecipientIds().size() : "broadcast");

        if (request.getRecipientIds() == null || request.getRecipientIds().isEmpty()) {
            log.warn("Bulk send called with empty recipient list.");
            return 0;
        }

        List<Notification> notifications = new ArrayList<>();
        String channel = request.getChannel() != null
                ? request.getChannel().toUpperCase() : AppConstants.CHANNEL_APP;

        for (Long recipientId : request.getRecipientIds()) {
            if (AppConstants.CHANNEL_APP.equals(channel) || AppConstants.CHANNEL_ALL.equals(channel)) {
                notifications.add(Notification.builder()
                        .recipientId(recipientId)
                        .type(request.getType())
                        .title(request.getTitle())
                        .message(request.getMessage())
                        .channel(AppConstants.CHANNEL_APP)
                        .relatedId(request.getRelatedId())
                        .relatedType(request.getRelatedType())
                        .deepLinkUrl(request.getDeepLinkUrl())
                        .isRead(false)
                        .build());
            }
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }

        log.info("Bulk notification sent to {} recipients", request.getRecipientIds().size());
        return request.getRecipientIds().size();
    }

    @Override
    public void markAsRead(Long notificationId, Long recipientId) {
        int updated = notificationRepository.markAsRead(notificationId, recipientId);
        if (updated == 0) {
            log.warn("markAsRead: notification {} not found for recipient {}", notificationId, recipientId);
        }
        log.debug("Notification {} marked as read by recipientId={}", notificationId, recipientId);
    }

    @Override
    public int markAllRead(Long recipientId) {
        int count = notificationRepository.markAllRead(recipientId);
        log.info("Marked {} notifications as read for recipientId={}", count, recipientId);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getByRecipient(Long recipientId,
                                                               Boolean unreadOnly,
                                                               Pageable pageable) {
        Page<NotificationResponse> page;
        if (Boolean.TRUE.equals(unreadOnly)) {
            page = notificationRepository
                    .findByRecipientIdAndIsReadOrderBySentAtDesc(recipientId, false, pageable)
                    .map(this::mapToResponse);
        } else {
            page = notificationRepository
                    .findByRecipientIdOrderBySentAtDesc(recipientId, pageable)
                    .map(this::mapToResponse);
        }
        return PagedResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    @Override
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteByNotificationId(notificationId);
        log.info("Notification {} deleted", notificationId);
    }

    @Override
    public void sendEmail(String toEmail, String subject, String body) {
        emailService.sendSimpleEmail(toEmail, subject, body);
    }

    @Override
    public void sendSms(String toPhone, String message) {
        smsService.sendSms(toPhone, message);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getAll(Pageable pageable) {
        Page<NotificationResponse> page = notificationRepository
                .findAllByOrderBySentAtDesc(pageable)
                .map(this::mapToResponse);
        return PagedResponse.of(page);
    }

    private void dispatchEmail(SendNotificationRequest request) {
        boolean queued = rabbitNotificationPublisher.publishEmail(request);
        if (!queued) {
            emailService.sendSimpleEmail(request.getRecipientEmail(), request.getTitle(), request.getMessage());
            log.debug("EMAIL notification sent directly to: {}", request.getRecipientEmail());
        }
    }

    private void dispatchSms(SendNotificationRequest request) {
        boolean queued = rabbitNotificationPublisher.publishSms(request);
        if (!queued) {
            smsService.sendSms(request.getRecipientPhone(), request.getMessage());
            log.debug("SMS notification sent directly to: {}", request.getRecipientPhone());
        }
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .recipientId(n.getRecipientId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .channel(n.getChannel())
                .relatedId(n.getRelatedId())
                .relatedType(n.getRelatedType())
                .deepLinkUrl(n.getDeepLinkUrl())
                .isRead(n.getIsRead())
                .audible(n.getAudible())
                .sentAt(n.getSentAt())
                .readAt(n.getReadAt())
                .build();
    }
}
