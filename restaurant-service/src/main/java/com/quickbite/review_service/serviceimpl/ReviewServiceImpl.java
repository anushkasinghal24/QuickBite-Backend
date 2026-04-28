package com.quickbite.review_service.serviceimpl;

import com.quickbite.review_service.dto.request.*;
import com.quickbite.review_service.dto.response.*;
import com.quickbite.review_service.entity.Review;
import com.quickbite.review_service.exception.*;
import com.quickbite.review_service.repository.ReviewRepository;
import com.quickbite.review_service.service.ReviewService;
import com.quickbite.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ReviewServiceImpl
 *
 * Complete business logic for Review/Rating-Service.
 * PDF Section 4.8 — all methods implemented.
 *
 * Key cross-service interactions:
 *  addReview()    → restaurant-service (update avgFoodRating)
 *  addReview()    → delivery-service (update avgDeliveryRating)
 *  addReview()    → notification-service (notify restaurant owner)
 *  updateReview() → restaurant-service + delivery-service (recompute & push)
 *  deleteReview() → restaurant-service + delivery-service (recompute & push)
 */
@Service
@RequiredArgsConstructor
@Slf4j
    @Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final RestaurantService restaurantService;
    private final RestTemplate restTemplate;

    @Value("${quickbite.delivery.base-url:http://localhost:8087}")
    private String deliveryBaseUrl;

    @Value("${quickbite.notification.base-url:http://localhost:8084}")
    private String notificationBaseUrl;

    // ═══════════════════════════════════════════════════════════════════
    // 1. ADD REVIEW
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public ReviewResponse addReview(Integer customerId, AddReviewRequest request) {
        log.info("Adding review by customerId={} for orderId={}", customerId, request.getOrderId());

        // ── Rule 1: One review per order ─────────────────────────────
        if (reviewRepository.existsByOrderId(request.getOrderId())) {
            throw new DuplicateReviewException(
                "A review already exists for orderId: " + request.getOrderId() +
                ". Only one review is allowed per order.");
        }

        // ── Rule 2: Validate deliveryRating only if agentId present ──
        if (request.getAgentId() != null && request.getDeliveryRating() == null) {
            throw new InvalidReviewException(
                "Delivery rating is required when agentId is provided.");
        }

        // ── Build and save review ─────────────────────────────────────
        Review review = Review.builder()
                .orderId(request.getOrderId())
                .customerId(customerId)
                .restaurantId(request.getRestaurantId())
                .agentId(request.getAgentId())
                .foodRating(request.getFoodRating())
                .deliveryRating(request.getDeliveryRating())
                .comment(request.getComment())
                .reviewDate(LocalDate.now())
                .isVerified(false)   // Requires admin verification
                .isFlagged(false)
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Review saved: reviewId={}", saved.getReviewId());

        // ── Push updated avgFoodRating to restaurant-service ──────────
        pushFoodRatingToRestaurant(request.getRestaurantId());

        // ── Push updated avgDeliveryRating to delivery-service ────────
        if (request.getAgentId() != null) {
            pushDeliveryRatingToAgent(request.getAgentId());
        }

        // ── Notify restaurant owner about new review ──────────────────
        sendNotification(
            "NEW_REVIEW",
            request.getRestaurantId(),
            "New Customer Review",
            "A customer has submitted a " + request.getFoodRating() +
            "★ review for your restaurant.",
            saved.getReviewId()
        );

        return ReviewResponse.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 2. GET BY RESTAURANT
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getByRestaurant(Integer restaurantId) {
        return reviewRepository
                .findTop10ByRestaurantIdAndIsVerifiedTrueOrderByCreatedAtDesc(restaurantId)
                .stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════
    // 3. GET BY CUSTOMER
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getByCustomer(Integer customerId) {
        return reviewRepository.findByCustomerId(customerId)
                .stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════
    // 4. GET BY ORDER
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getByOrder(Integer orderId) {
        Review review = reviewRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ReviewNotFoundException(
                    "No review found for orderId: " + orderId));
        return ReviewResponse.from(review);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 5. GET BY AGENT
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getByAgent(Integer agentId) {
        return reviewRepository.findByAgentId(agentId)
                .stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════
    // 6. UPDATE REVIEW
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public ReviewResponse updateReview(Integer reviewId, Integer customerId,
                                        UpdateReviewRequest request) {
        log.info("Updating reviewId={} by customerId={}", reviewId, customerId);

        Review review = findById(reviewId);

        // Only the original reviewer can update their review
        if (!review.getCustomerId().equals(customerId)) {
            throw new UnauthorizedReviewException(
                "You can only update your own reviews.");
        }

        // Update only non-null fields
        if (request.getFoodRating() != null)     review.setFoodRating(request.getFoodRating());
        if (request.getDeliveryRating() != null) review.setDeliveryRating(request.getDeliveryRating());
        if (request.getComment() != null)        review.setComment(request.getComment());

        // Mark as needing re-verification after edit
        review.setIsVerified(false);

        Review saved = reviewRepository.save(review);
        log.info("Review updated: reviewId={}", saved.getReviewId());

        // Re-push updated averages
        pushFoodRatingToRestaurant(review.getRestaurantId());
        if (review.getAgentId() != null) {
            pushDeliveryRatingToAgent(review.getAgentId());
        }

        return ReviewResponse.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 7. DELETE REVIEW
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void deleteReview(Integer reviewId) {
        Review review = findById(reviewId);

        Integer restaurantId = review.getRestaurantId();
        Integer agentId = review.getAgentId();

        reviewRepository.delete(review);
        log.info("Review deleted: reviewId={}", reviewId);

        // Recompute and push averages after deletion
        pushFoodRatingToRestaurant(restaurantId);
        if (agentId != null) {
            pushDeliveryRatingToAgent(agentId);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 8. GET AVG FOOD RATING
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Double getAvgFoodRating(Integer restaurantId) {
        Double avg = reviewRepository.avgFoodRatingByRestaurantId(restaurantId);
        return avg != null ? Math.round(avg * 100.0) / 100.0 : 0.0;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 9. GET AVG DELIVERY RATING
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Double getAvgDeliveryRating(Integer agentId) {
        Double avg = reviewRepository.avgDeliveryRatingByAgentId(agentId);
        return avg != null ? Math.round(avg * 100.0) / 100.0 : 0.0;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 10. GET ALL REVIEWS (ADMIN)
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAll()
                .stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════
    // 11. FLAG REVIEW
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public ReviewResponse flagReview(Integer reviewId, FlagReviewRequest request) {
        Review review = findById(reviewId);

        review.setIsFlagged(true);
        review.setFlagReason(request.getReason());
        // Temporarily unverify until admin reviews it
        review.setIsVerified(false);

        Review saved = reviewRepository.save(review);
        log.info("Review flagged: reviewId={}, reason={}", reviewId, request.getReason());

        return ReviewResponse.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 12. VERIFY REVIEW (ADMIN)
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public ReviewResponse verifyReview(Integer reviewId) {
        Review review = findById(reviewId);

        review.setIsVerified(true);
        review.setIsFlagged(false);
        review.setFlagReason(null);

        Review saved = reviewRepository.save(review);
        log.info("Review verified by admin: reviewId={}", reviewId);

        // Re-push averages now that review is verified (only verified reviews count)
        pushFoodRatingToRestaurant(review.getRestaurantId());
        if (review.getAgentId() != null) {
            pushDeliveryRatingToAgent(review.getAgentId());
        }

        return ReviewResponse.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 13. GET RESTAURANT RATING SUMMARY
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public RatingAverageResponse getRestaurantRatingSummary(Integer restaurantId) {
        Double avg   = getAvgFoodRating(restaurantId);
        long total   = reviewRepository.countByRestaurantId(restaurantId);
        return RatingAverageResponse.builder()
                .entityId(restaurantId)
                .entityType("RESTAURANT")
                .avgRating(avg)
                .totalReviews(total)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    // 14. GET AGENT RATING SUMMARY
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public RatingAverageResponse getAgentRatingSummary(Integer agentId) {
        Double avg   = getAvgDeliveryRating(agentId);
        long total   = reviewRepository.countByAgentId(agentId);
        return RatingAverageResponse.builder()
                .entityId(agentId)
                .entityType("AGENT")
                .avgRating(avg)
                .totalReviews(total)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    // 15. GET FLAGGED REVIEWS (ADMIN)
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getFlaggedReviews() {
        return reviewRepository.findByIsFlaggedTrue()
                .stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private Review findById(Integer reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(
                    "Review not found with reviewId: " + reviewId));
    }

    /**
     * Recompute avgFoodRating and push to restaurant-service.
     * PDF: "Average ratings computed and pushed back to Restaurant-Service."
     * Only verified reviews count towards the average.
     */
    private void pushFoodRatingToRestaurant(Integer restaurantId) {
        try {
            Double avg = getAvgFoodRating(restaurantId);
            com.quickbite.restaurant.dto.request.UpdateRatingRequest updateRatingRequest =
                    new com.quickbite.restaurant.dto.request.UpdateRatingRequest();
            updateRatingRequest.setAvgRating(avg);
            updateRatingRequest.setTotalReviews((int) reviewRepository.countByRestaurantId(restaurantId));
            restaurantService.updateRating(restaurantId.longValue(), updateRatingRequest);
            log.info("Pushed avgFoodRating={} to restaurant-service for restaurantId={}",
                    avg, restaurantId);
        } catch (Exception e) {
            // Non-blocking — fallback handles logging
            log.warn("Failed to push food rating to restaurant-service: {}", e.getMessage());
        }
    }

    /**
     * Recompute avgDeliveryRating and push to delivery-service.
     * PDF: "Average ratings computed and pushed back to Delivery-Agent-Service."
     */
    private void pushDeliveryRatingToAgent(Integer agentId) {
        try {
            Double avg = getAvgDeliveryRating(agentId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String authHeader = currentAuthorizationHeader();
            if (authHeader != null) {
                headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            }
            restTemplate.put(
                    deliveryBaseUrl + "/api/v1/agents/{agentId}/rating",
                    new HttpEntity<>(Map.of("avgRating", avg), headers),
                    agentId);
            log.info("Pushed avgDeliveryRating={} to delivery-service for agentId={}",
                    avg, agentId);
        } catch (Exception e) {
            log.warn("Failed to push delivery rating to delivery-service: {}", e.getMessage());
        }
    }

    /** Send notification via notification-service */
    private void sendNotification(String type, Integer recipientId,
                                   String title, String message, Integer relatedId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String authHeader = currentAuthorizationHeader();
            if (authHeader != null) {
                headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            }
            restTemplate.postForEntity(
                    notificationBaseUrl + "/api/v1/notifications/send",
                    new HttpEntity<>(Map.of(
                "type",        type,
                "recipientId", recipientId,
                "title",       title,
                "message",     message,
                "relatedId",   relatedId,
                "relatedType", "REVIEW"
            ), headers),
                    String.class);
        } catch (Exception e) {
            log.warn("Notification failed (non-critical): {}", e.getMessage());
        }
    }

    private String currentAuthorizationHeader() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader(HttpHeaders.AUTHORIZATION);
    }
}
