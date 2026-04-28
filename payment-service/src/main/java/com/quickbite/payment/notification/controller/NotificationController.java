package com.quickbite.payment.notification.controller;

import com.quickbite.payment.dto.response.ApiResponse;
import com.quickbite.payment.dto.response.PagedResponse;
import com.quickbite.payment.notification.dto.request.*;
import com.quickbite.payment.notification.dto.response.*;
import com.quickbite.payment.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * NotificationController â€” PDF Section 4.9
 *
 * Base URL: /api/v1/notifications
 *
 * â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * ENDPOINTS:
 *
 * POST /send               â†’ Send single notification (all services call this)
 * POST /send-bulk          â†’ Admin broadcast
 * GET  /recipient/{id}     â†’ Get notifications for a user (paginated)
 * GET  /unread-count/{id}  â†’ Unread badge count (PDF 2.7)
 * PUT  /read/{id}          â†’ Mark single as read
 * PUT  /read-all/{recipientId} â†’ Mark ALL as read
 * DELETE /{id}             â†’ Delete notification
 * GET  /all                â†’ Admin: all notifications
 * â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 *
 * Swagger: http://localhost:8088/swagger-ui.html
 *
 * Called by services (via Feign):
 *   order-service    â†’ ORDER_PLACED, ORDER_CONFIRMED, ORDER_PREPARING,
 *                       ORDER_PICKED_UP, ORDER_DELIVERED, ORDER_CANCELLED
 *   payment-service  â†’ PAYMENT_RECEIPT, REFUND_INITIATED, WALLET_TOPUP
 *   restaurant-service â†’ RESTAURANT_APPROVED, RESTAURANT_REJECTED, NEW_ORDER_ALERT
 *   delivery-service â†’ AGENT_VERIFIED, AGENT_ASSIGNED
 *   review-service   â†’ (future) review response notification
 *   admin            â†’ POST /send-bulk (promotions)
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification API", description = "In-app, email, and SMS notification management")
public class NotificationController {

    private final NotificationService notificationService;

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // SEND â€” called by all microservices
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * POST /api/v1/notifications/send
     *
     * Universal endpoint â€” every service calls this to dispatch notifications.
     * Supports APP / EMAIL / SMS / ALL channels.
     *
     * Request examples:
     *   order-service on order placed:
     *     { recipientId:10, type:"ORDER_PLACED", title:"Order Placed!",
     *       message:"Your order #123 placed.", channel:"ALL",
     *       relatedId:123, relatedType:"ORDER",
     *       deepLinkUrl:"/orders/123",
     *       recipientEmail:"user@example.com", recipientPhone:"+919876543210" }
     *
     *   payment-service on payment receipt:
     *     { recipientId:10, type:"PAYMENT_RECEIPT", channel:"APP",
     *       title:"Payment Successful", message:"â‚¹350 paid for order #123" }
     *
     *   restaurant-service on new order (NEW_ORDER_ALERT):
     *     { recipientId:5, type:"NEW_ORDER_ALERT", channel:"APP",
     *       title:"New Order!", message:"New order #123 received!",
     *       audible:true }
    */
    @PostMapping("/send")
    @Operation(summary = "Send notification (called by all microservices)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<NotificationResponse>> send(
            @Valid @RequestBody SendNotificationRequest request) {

        log.info("Send notification: type={}, channel={}, recipientId={}",
                request.getType(), request.getChannel(), request.getRecipientId());
        NotificationResponse response = notificationService.send(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notification sent.", response));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // SEND BULK â€” admin broadcast (PDF Section 2.7)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * POST /api/v1/notifications/send-bulk
     *
     * Admin broadcast: "Send platform-wide notifications (promotions, maintenance alerts)"
     * Provide list of recipientIds or broadcastAll=true.
     */
    @PostMapping("/send-bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk send / admin broadcast (ADMIN only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Integer>> sendBulk(
            @Valid @RequestBody BulkNotificationRequest request) {

        log.info("Bulk notification: type={}, recipients={}",
                request.getType(),
                request.getRecipientIds() != null ? request.getRecipientIds().size() : "broadcast");
        int count = notificationService.sendBulk(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bulk notification sent to " + count + " recipients.", count));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET NOTIFICATIONS FOR A RECIPIENT
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * GET /api/v1/notifications/recipient/{recipientId}?page=0&size=20&unreadOnly=false
     *
     * Customer / Owner / Agent views their own notification centre.
     * unreadOnly=true â†’ only unread notifications.
     */
    @GetMapping("/recipient/{recipientId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','OWNER','AGENT','ADMIN')")
    @Operation(summary = "Get notifications for a recipient (paginated)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getByRecipient(
            @PathVariable Long recipientId,
            @RequestParam(defaultValue = "false") Boolean unreadOnly,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        // Security: customers can only see their own notifications
        Long authenticatedUserId = (Long) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !authenticatedUserId.equals(recipientId)) {
            log.warn("User {} attempted to view notifications of {}", authenticatedUserId, recipientId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You can only view your own notifications.", "ACCESS_DENIED"));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched.",
                notificationService.getByRecipient(recipientId, unreadOnly, pageable)));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // UNREAD COUNT â€” PDF Section 2.7
    // "Unread badge count displayed in real time in the navigation bar."
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * GET /api/v1/notifications/unread-count/{recipientId}
     *
     * Polled by frontend every ~30 seconds to update nav bar badge.
     * Returns a single Long number.
     */
    @GetMapping("/unread-count/{recipientId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','OWNER','AGENT','ADMIN')")
    @Operation(summary = "Get unread notification count (nav bar badge)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @PathVariable Long recipientId,
            Authentication authentication) {

        Long authenticatedUserId = (Long) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !authenticatedUserId.equals(recipientId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied.", "ACCESS_DENIED"));
        }

        return ResponseEntity.ok(ApiResponse.success("Unread count.",
                notificationService.getUnreadCount(recipientId)));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // MARK AS READ
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * PUT /api/v1/notifications/read/{notificationId}?recipientId=10
     * Mark single notification as read.
     */
    @PutMapping("/read/{notificationId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','OWNER','AGENT','ADMIN')")
    @Operation(summary = "Mark single notification as read",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication) {

        Long recipientId = (Long) authentication.getPrincipal();
        notificationService.markAsRead(notificationId, recipientId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read."));
    }

    /**
     * PUT /api/v1/notifications/read-all/{recipientId}
     * Mark ALL notifications as read for a recipient.
     * Called when user opens notification centre.
     */
    @PutMapping("/read-all/{recipientId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','OWNER','AGENT','ADMIN')")
    @Operation(summary = "Mark all notifications as read",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Integer>> markAllRead(
            @PathVariable Long recipientId,
            Authentication authentication) {

        Long authenticatedUserId = (Long) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !authenticatedUserId.equals(recipientId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied.", "ACCESS_DENIED"));
        }

        int count = notificationService.markAllRead(recipientId);
        return ResponseEntity.ok(ApiResponse.success(count + " notifications marked as read.", count));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // DELETE
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * DELETE /api/v1/notifications/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','OWNER','AGENT','ADMIN')")
    @Operation(summary = "Delete a notification",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted."));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // ADMIN: ALL NOTIFICATIONS
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * GET /api/v1/notifications/all?page=0&size=20
     * Admin sees all notifications platform-wide.
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all notifications â€” ADMIN only",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        return ResponseEntity.ok(ApiResponse.success("All notifications fetched.",
                notificationService.getAll(pageable)));
    }
}
