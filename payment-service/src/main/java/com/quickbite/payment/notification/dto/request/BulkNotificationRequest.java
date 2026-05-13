package com.quickbite.payment.notification.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

/**
 * BulkNotificationRequest
 * POST /api/v1/notifications/send-bulk
 *
 * PDF Section 2.7:
 *   "Admin can broadcast platform-wide promotional or informational notifications."
 *
 * Also used by:
 *   - order-service: notify customer + restaurant owner + agent simultaneously
 *   - admin: promotional campaigns
 */
@Data
public class BulkNotificationRequest {

    /** List of recipient userIds. If empty + broadcastAll=true, sends to all. */
    private List<Long> recipientIds;

    /** If true, sends to ALL registered users (admin broadcast only) */
    private Boolean broadcastAll = false;

    /** Filter by role for broadcast: CUSTOMER / OWNER / AGENT / null (all) */
    private String targetRole;

    @NotBlank(message = "type is required")
    private String type;

    @NotBlank(message = "title is required")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "message is required")
    @Size(max = 500)
    private String message;

    /** APP / EMAIL / SMS / ALL */
    private String channel = "APP";

    private Long relatedId;
    private String relatedType;
    private String deepLinkUrl;
}
