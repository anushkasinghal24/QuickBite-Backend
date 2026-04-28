package com.quickbite.payment.notification.repository;

import com.quickbite.payment.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * NotificationRepository â€” PDF Section 4.9
 *
 * Methods declared:
 *   findByRecipientId, findByRecipientIdAndIsRead,
 *   countByRecipientIdAndIsRead, findByType,
 *   findByRelatedId, deleteByNotificationId
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // â”€â”€ Get by recipient (sorted newest first) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    List<Notification> findByRecipientIdOrderBySentAtDesc(Long recipientId);

    Page<Notification> findByRecipientIdOrderBySentAtDesc(Long recipientId, Pageable pageable);

    // â”€â”€ Unread only â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    List<Notification> findByRecipientIdAndIsReadFalseOrderBySentAtDesc(Long recipientId);

    Page<Notification> findByRecipientIdAndIsReadOrderBySentAtDesc(
            Long recipientId, Boolean isRead, Pageable pageable);

    // â”€â”€ Unread badge count â€” PDF Section 2.7 â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    long countByRecipientIdAndIsReadFalse(Long recipientId);

    // â”€â”€ By type â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    List<Notification> findByTypeOrderBySentAtDesc(String type);

    Page<Notification> findByTypeOrderBySentAtDesc(String type, Pageable pageable);

    // â”€â”€ By related entity (orderId, paymentId, etc.) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    List<Notification> findByRelatedIdOrderBySentAtDesc(Long relatedId);

    List<Notification> findByRelatedIdAndRelatedType(Long relatedId, String relatedType);

    // â”€â”€ Mark single as read â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP " +
           "WHERE n.notificationId = :id AND n.recipientId = :recipientId")
    int markAsRead(@Param("id") Long id, @Param("recipientId") Long recipientId);

    // â”€â”€ Mark ALL as read for a recipient â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP " +
           "WHERE n.recipientId = :recipientId AND n.isRead = false")
    int markAllRead(@Param("recipientId") Long recipientId);

    // â”€â”€ Delete by id â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    void deleteByNotificationId(Long notificationId);

    // â”€â”€ Admin: all notifications paginated â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    Page<Notification> findAllByOrderBySentAtDesc(Pageable pageable);

    // â”€â”€ Exists check â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    boolean existsByRecipientIdAndRelatedIdAndType(Long recipientId, Long relatedId, String type);
}
