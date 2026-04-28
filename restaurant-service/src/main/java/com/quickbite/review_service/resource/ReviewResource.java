package com.quickbite.review_service.resource;

import com.quickbite.review_service.dto.request.*;
import com.quickbite.review_service.dto.response.*;
import com.quickbite.review_service.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ReviewResource — REST Controller
 *
 * PDF Section 4.8:
 * ReviewResource exposes /reviews endpoints:
 *   POST (add), GET (by restaurant/customer/order/agent/all),
 *   PUT (update), DELETE, GET (avgFood/avgDelivery)
 *
 * Base URL: /api/v1/reviews
 * Gateway route: /api/v1/reviews/** → lb://review-service
 *
 * ┌────────────────────────────────────────────────────┬────────────┬──────────────────┐
 * │ Endpoint                                            │ Auth       │ Role             │
 * ├────────────────────────────────────────────────────┼────────────┼──────────────────┤
 * │ POST   /                                           │ JWT        │ CUSTOMER         │
 * │ GET    /restaurant/{restaurantId}                  │ PUBLIC     │ Guest/Any        │
 * │ GET    /customer/{customerId}                      │ JWT        │ CUSTOMER         │
 * │ GET    /order/{orderId}                            │ JWT        │ CUSTOMER/OWNER   │
 * │ GET    /agent/{agentId}                            │ JWT        │ AGENT/ADMIN      │
 * │ GET    /all                                        │ JWT        │ ADMIN            │
 * │ GET    /flagged                                    │ JWT        │ ADMIN            │
 * │ PUT    /{reviewId}                                 │ JWT        │ CUSTOMER (own)   │
 * │ DELETE /{reviewId}                                 │ JWT        │ ADMIN/CUSTOMER   │
 * │ PUT    /{reviewId}/flag                            │ JWT        │ OWNER/ADMIN      │
 * │ PUT    /{reviewId}/verify                          │ JWT        │ ADMIN            │
 * │ GET    /restaurant/{restaurantId}/average          │ PUBLIC     │ Any              │
 * │ GET    /agent/{agentId}/average                    │ JWT        │ Any              │
 * │ GET    /restaurant/{restaurantId}/summary          │ PUBLIC     │ Any              │
 * │ GET    /agent/{agentId}/summary                    │ JWT        │ Any              │
 * └────────────────────────────────────────────────────┴────────────┴──────────────────┘
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Review & Rating API", description = "Dual rating system — food quality + delivery experience")
public class ReviewResource {

    private final ReviewService reviewService;

    // ─────────────────────────────────────────────────────────────────
    // POST /api/v1/reviews
    // Customer submits review after order completion
    // ─────────────────────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Submit review after order completion (CUSTOMER)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ReviewResponse>> addReview(
            @Valid @RequestBody AddReviewRequest request,
            Authentication authentication) {

        Integer customerId = (Integer) authentication.getPrincipal();
        log.info("POST /api/v1/reviews — customerId={}, orderId={}", customerId, request.getOrderId());

        ReviewResponse response = reviewService.addReview(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully. Awaiting verification.", response));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/reviews/restaurant/{restaurantId}
    // PUBLIC — guests can read restaurant reviews
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/restaurant/{restaurantId}")
    @Operation(summary = "Get reviews for a restaurant (PUBLIC)")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getByRestaurant(
            @PathVariable Integer restaurantId) {

        List<ReviewResponse> reviews = reviewService.getByRestaurant(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(
                "Reviews fetched for restaurant: " + restaurantId, reviews));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/reviews/customer/{customerId}
    // Customer views own reviews
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get reviews by customer (CUSTOMER)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getByCustomer(
            @PathVariable Integer customerId) {

        List<ReviewResponse> reviews = reviewService.getByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(
                "Reviews fetched for customer: " + customerId, reviews));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/reviews/order/{orderId}
    // Get review for a specific order
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get review for an order",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ReviewResponse>> getByOrder(
            @PathVariable Integer orderId) {

        ReviewResponse response = reviewService.getByOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success("Review fetched", response));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/reviews/agent/{agentId}
    // Delivery agent views their own ratings
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/agent/{agentId}")
    @Operation(summary = "Get reviews for a delivery agent (AGENT/ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getByAgent(
            @PathVariable Integer agentId) {

        List<ReviewResponse> reviews = reviewService.getByAgent(agentId);
        return ResponseEntity.ok(ApiResponse.success(
                "Reviews fetched for agent: " + agentId, reviews));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/reviews/all  — ADMIN
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all reviews (ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getAllReviews() {
        List<ReviewResponse> reviews = reviewService.getAllReviews();
        return ResponseEntity.ok(ApiResponse.success(
                "Total reviews: " + reviews.size(), reviews));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/reviews/flagged  — ADMIN
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/flagged")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all flagged reviews awaiting moderation (ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getFlaggedReviews() {
        List<ReviewResponse> reviews = reviewService.getFlaggedReviews();
        return ResponseEntity.ok(ApiResponse.success(
                "Flagged reviews: " + reviews.size(), reviews));
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /api/v1/reviews/{reviewId}
    // Customer updates their own review
    // ─────────────────────────────────────────────────────────────────
    @PutMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update own review (CUSTOMER)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Integer reviewId,
            @Valid @RequestBody UpdateReviewRequest request,
            Authentication authentication) {

        Integer customerId = (Integer) authentication.getPrincipal();
        log.info("PUT /api/v1/reviews/{} — customerId={}", reviewId, customerId);

        ReviewResponse response = reviewService.updateReview(reviewId, customerId, request);
        return ResponseEntity.ok(ApiResponse.success("Review updated", response));
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE /api/v1/reviews/{reviewId}
    // ADMIN deletes (moderation) or CUSTOMER deletes own
    // ─────────────────────────────────────────────────────────────────
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Operation(summary = "Delete review (ADMIN moderation or CUSTOMER own)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Integer reviewId) {
        log.info("DELETE /api/v1/reviews/{}", reviewId);
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted"));
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /api/v1/reviews/{reviewId}/flag
    // Owner or Admin flags a review for moderation
    // ─────────────────────────────────────────────────────────────────
    @PutMapping("/{reviewId}/flag")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Flag review for moderation (OWNER/ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ReviewResponse>> flagReview(
            @PathVariable Integer reviewId,
            @Valid @RequestBody FlagReviewRequest request) {

        log.info("PUT /api/v1/reviews/{}/flag — reason={}", reviewId, request.getReason());
        ReviewResponse response = reviewService.flagReview(reviewId, request);
        return ResponseEntity.ok(ApiResponse.success("Review flagged for moderation", response));
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /api/v1/reviews/{reviewId}/verify
    // Admin verifies/approves a review (marks as clean)
    // ─────────────────────────────────────────────────────────────────
    @PutMapping("/{reviewId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Verify/approve a review (ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ReviewResponse>> verifyReview(
            @PathVariable Integer reviewId) {

        log.info("PUT /api/v1/reviews/{}/verify", reviewId);
        ReviewResponse response = reviewService.verifyReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review verified", response));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/reviews/restaurant/{restaurantId}/average
    // Get avg food rating for a restaurant — PUBLIC
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/restaurant/{restaurantId}/average")
    @Operation(summary = "Get average food rating for restaurant (PUBLIC)")
    public ResponseEntity<ApiResponse<Double>> getAvgFoodRating(
            @PathVariable Integer restaurantId) {

        Double avg = reviewService.getAvgFoodRating(restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Average food rating", avg));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/reviews/agent/{agentId}/average
    // Get avg delivery rating for an agent
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/agent/{agentId}/average")
    @Operation(summary = "Get average delivery rating for agent",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Double>> getAvgDeliveryRating(
            @PathVariable Integer agentId) {

        Double avg = reviewService.getAvgDeliveryRating(agentId);
        return ResponseEntity.ok(ApiResponse.success("Average delivery rating", avg));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/reviews/restaurant/{restaurantId}/summary
    // Full rating summary (avg + count) — PUBLIC
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/restaurant/{restaurantId}/summary")
    @Operation(summary = "Get rating summary for restaurant (avg + count, PUBLIC)")
    public ResponseEntity<ApiResponse<RatingAverageResponse>> getRestaurantSummary(
            @PathVariable Integer restaurantId) {

        RatingAverageResponse summary = reviewService.getRestaurantRatingSummary(restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Restaurant rating summary", summary));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/reviews/agent/{agentId}/summary
    // Full rating summary for agent
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/agent/{agentId}/summary")
    @Operation(summary = "Get rating summary for agent (avg + count)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<RatingAverageResponse>> getAgentSummary(
            @PathVariable Integer agentId) {

        RatingAverageResponse summary = reviewService.getAgentRatingSummary(agentId);
        return ResponseEntity.ok(ApiResponse.success("Agent rating summary", summary));
    }
}
