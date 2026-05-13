package com.quickbite.payment.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {
    private Long notificationId;
    private Long recipientId;
    private String type;
    private String title;
    private String message;
    private String channel;
    private Long relatedId;
    private String relatedType;
    private String deepLinkUrl;
    private Boolean isRead;
    private Boolean audible;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}
