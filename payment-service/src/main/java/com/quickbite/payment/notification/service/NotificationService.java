package com.quickbite.payment.notification.service;

import com.quickbite.payment.dto.response.PagedResponse;
import com.quickbite.payment.notification.dto.request.*;
import com.quickbite.payment.notification.dto.response.*;
import org.springframework.data.domain.Pageable;

/**
 * NotificationService Interface â€” PDF Section 4.9
 *
 * Declared methods from class diagram:
 *   send(), sendBulk(), markAsRead(), markAllRead(),
 *   getByRecipient(), getUnreadCount(), deleteNotification(),
 *   sendEmail(), sendSMS(), getAll()
 */
public interface NotificationService {

    /** Send single notification (APP / EMAIL / SMS / ALL) */
    NotificationResponse send(SendNotificationRequest request);

    /** Bulk send â€” admin broadcast (PDF 2.7: "Admin can broadcast platform-wide") */
    int sendBulk(BulkNotificationRequest request);

    /** Mark single notification as read */
    void markAsRead(Long notificationId, Long recipientId);

    /** Mark ALL notifications as read for a recipient */
    int markAllRead(Long recipientId);

    /** Get all notifications for a recipient (paginated, newest first) */
    PagedResponse<NotificationResponse> getByRecipient(Long recipientId, Boolean unreadOnly, Pageable pageable);

    /**
     * Get unread badge count â€” PDF Section 2.7:
     * "Unread badge count displayed in real time in the navigation bar."
     */
    long getUnreadCount(Long recipientId);

    /** Delete a notification */
    void deleteNotification(Long notificationId);

    /** Direct email dispatch (internal use) */
    void sendEmail(String toEmail, String subject, String body);

    /** Direct SMS dispatch (internal use) */
    void sendSms(String toPhone, String message);

    /** Admin: get all notifications paginated */
    PagedResponse<NotificationResponse> getAll(Pageable pageable);
}
