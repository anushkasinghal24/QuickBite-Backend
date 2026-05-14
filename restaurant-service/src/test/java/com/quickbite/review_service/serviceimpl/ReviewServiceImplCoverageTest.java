package com.quickbite.review_service.serviceimpl;

import com.quickbite.restaurant.feign.DeliveryServiceClient;
import com.quickbite.restaurant.feign.NotificationServiceClient;
import com.quickbite.restaurant.service.RabbitNotificationPublisher;
import com.quickbite.restaurant.service.RestaurantService;
import com.quickbite.review_service.dto.request.AddReviewRequest;
import com.quickbite.review_service.dto.request.FlagReviewRequest;
import com.quickbite.review_service.dto.request.UpdateReviewRequest;
import com.quickbite.review_service.dto.response.ApiResponse;
import com.quickbite.review_service.dto.response.OrderDetailsDTO;
import com.quickbite.review_service.dto.response.RatingAverageResponse;
import com.quickbite.review_service.dto.response.ReviewResponse;
import com.quickbite.review_service.entity.Review;
import com.quickbite.review_service.exception.InvalidReviewException;
import com.quickbite.review_service.feign.OrderServiceClient;
import com.quickbite.review_service.repository.ReviewRepository;
import com.quickbite.review_service.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplCoverageTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private OrderServiceClient orderServiceClient;
    @Mock private RestaurantService restaurantService;
    @Mock private DeliveryServiceClient deliveryServiceClient;
    @Mock private NotificationServiceClient notificationServiceClient;
    @Mock private RabbitNotificationPublisher rabbitNotificationPublisher;
    @Mock private ReviewService self;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private OrderDetailsDTO deliveredOrder;

    @BeforeEach
    void setUp() {
        deliveredOrder = OrderDetailsDTO.builder()
                .orderId(900)
                .customerId(11)
                .restaurantId(77)
                .deliveryAgentId(88)
                .orderStatus("DELIVERED")
                .restaurantName("Spice House")
                .customerName("Aman Verma")
                .build();

        lenient().when(self.getAvgFoodRating(77)).thenReturn(4.5);
        lenient().when(self.getAvgDeliveryRating(88)).thenReturn(4.0);
        lenient().when(reviewRepository.countByRestaurantId(77)).thenReturn(12L);
        lenient().doNothing().when(restaurantService).updateRating(eq(77L), any());
        lenient().doNothing().when(deliveryServiceClient).updateAgentRating(eq(88), anyMap());
        lenient().doNothing().when(notificationServiceClient).sendNotification(anyMap());
    }

    @Test
    void addReview_shouldPersistReviewAndFallbackToNotificationClient() {
        AddReviewRequest request = AddReviewRequest.builder()
                .orderId(900)
                .restaurantId(77)
                .agentId(88)
                .foodRating(5)
                .deliveryRating(4)
                .comment("Loved the food")
                .build();

        ApiResponse<OrderDetailsDTO> orderResponse = ApiResponse.<OrderDetailsDTO>builder()
                .success(true)
                .message("ok")
                .data(deliveredOrder)
                .build();

        when(orderServiceClient.getOrderById(900)).thenReturn(orderResponse);
        when(reviewRepository.existsByOrderId(900)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setReviewId(321);
            return review;
        });
        when(rabbitNotificationPublisher.publish(any(Map.class))).thenReturn(false);

        ReviewResponse response = reviewService.addReview(11, request);

        assertThat(response.getReviewId()).isEqualTo(321);
        assertThat(response.getOrderId()).isEqualTo(900);
        assertThat(response.getRestaurantId()).isEqualTo(77);
        assertThat(response.getFoodRating()).isEqualTo(5);
        assertThat(response.getDeliveryRating()).isEqualTo(4);
        verify(restaurantService).updateRating(eq(77L), any());
        verify(deliveryServiceClient).updateAgentRating(eq(88), anyMap());
        verify(notificationServiceClient).sendNotification(anyMap());
    }

    @Test
    void addReview_shouldRejectWhenAgentProvidedWithoutDeliveryRating() {
        AddReviewRequest request = AddReviewRequest.builder()
                .orderId(901)
                .restaurantId(77)
                .agentId(88)
                .foodRating(5)
                .comment("Great")
                .build();

        ApiResponse<OrderDetailsDTO> orderResponse = ApiResponse.<OrderDetailsDTO>builder()
                .success(true)
                .message("ok")
                .data(OrderDetailsDTO.builder()
                        .orderId(901)
                        .customerId(11)
                        .restaurantId(77)
                        .deliveryAgentId(88)
                        .orderStatus("DELIVERED")
                        .restaurantName("Spice House")
                        .customerName("Aman Verma")
                        .build())
                .build();

        when(orderServiceClient.getOrderById(901)).thenReturn(orderResponse);
        lenient().when(reviewRepository.existsByOrderId(901)).thenReturn(false);

        assertThrows(InvalidReviewException.class, () -> reviewService.addReview(11, request));
    }

    @Test
    void getByOrder_shouldReturnMappedReview() {
        Review review = review(700, 900, 11, 77, 88, 5, 4, "Excellent", true, false);
        when(reviewRepository.findByOrderId(900)).thenReturn(Optional.of(review));

        ReviewResponse response = reviewService.getByOrder(900);

        assertEquals(700, response.getReviewId());
        assertEquals(900, response.getOrderId());
        assertEquals(77, response.getRestaurantId());
        assertEquals(88, response.getAgentId());
        assertEquals("Excellent", response.getComment());
    }

    @Test
    void getByAgent_shouldReturnMappedReviews() {
        Review first = review(701, 901, 11, 77, 88, 4, 5, "Nice", true, false);
        Review second = review(702, 902, 12, 77, 88, 5, 5, "Great", true, false);
        when(reviewRepository.findByAgentId(88)).thenReturn(List.of(first, second));

        List<ReviewResponse> responses = reviewService.getByAgent(88);

        assertThat(responses).hasSize(2);
        assertEquals(701, responses.get(0).getReviewId());
        assertEquals(702, responses.get(1).getReviewId());
    }

    @Test
    void updateReview_shouldUpdateMutableFieldsAndPushRecomputedRatings() {
        Review existing = review(703, 903, 11, 77, 88, 4, 4, "Okay", true, false);
        when(reviewRepository.findById(703)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateReviewRequest request = UpdateReviewRequest.builder()
                .foodRating(5)
                .deliveryRating(3)
                .comment("Updated review")
                .build();

        ReviewResponse response = reviewService.updateReview(703, 11, request);

        assertEquals(5, response.getFoodRating());
        assertEquals(3, response.getDeliveryRating());
        assertEquals("Updated review", response.getComment());
        verify(restaurantService).updateRating(eq(77L), any());
        verify(deliveryServiceClient).updateAgentRating(eq(88), anyMap());
    }

    @Test
    void deleteReview_shouldDeleteAsAdminAndRecomputeRatings() {
        Review existing = review(704, 904, 11, 77, 88, 5, 4, "Delete me", true, false);
        when(reviewRepository.findById(704)).thenReturn(Optional.of(existing));

        reviewService.deleteReview(704, 999, "ADMIN");

        verify(reviewRepository).delete(existing);
        verify(restaurantService).updateRating(eq(77L), any());
        verify(deliveryServiceClient).updateAgentRating(eq(88), anyMap());
    }

    @Test
    void getRestaurantRatingSummary_shouldUseAverageAndCount() {
        when(self.getAvgFoodRating(77)).thenReturn(4.25);
        when(reviewRepository.countByRestaurantId(77)).thenReturn(9L);

        RatingAverageResponse response = reviewService.getRestaurantRatingSummary(77);

        assertEquals(77, response.getEntityId());
        assertEquals("RESTAURANT", response.getEntityType());
        assertEquals(4.25, response.getAvgRating());
        assertEquals(9L, response.getTotalReviews());
    }

    @Test
    void getAgentRatingSummary_shouldUseAverageAndCount() {
        when(self.getAvgDeliveryRating(88)).thenReturn(4.75);
        when(reviewRepository.countByAgentId(88)).thenReturn(6L);

        RatingAverageResponse response = reviewService.getAgentRatingSummary(88);

        assertEquals(88, response.getEntityId());
        assertEquals("AGENT", response.getEntityType());
        assertEquals(4.75, response.getAvgRating());
        assertEquals(6L, response.getTotalReviews());
    }

    @Test
    void getFlaggedReviews_shouldReturnMappedFlaggedReviews() {
        Review flagged = review(705, 905, 11, 77, 88, 1, 1, "Spam", true, true);
        when(reviewRepository.findByIsFlaggedTrue()).thenReturn(List.of(flagged));

        List<ReviewResponse> responses = reviewService.getFlaggedReviews();

        assertThat(responses).hasSize(1);
        assertEquals(705, responses.get(0).getReviewId());
        assertThat(responses.get(0).getIsFlagged()).isTrue();
    }

    @Test
    void verifyReview_shouldClearFlagsAndPushRatings() {
        Review existing = review(706, 906, 11, 77, 88, 3, 4, "Needs verification", false, true);
        existing.setFlagReason("spam");
        when(reviewRepository.findById(706)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.verifyReview(706);

        assertEquals(706, response.getReviewId());
        assertThat(response.getIsVerified()).isTrue();
        assertThat(response.getIsFlagged()).isFalse();
        verify(restaurantService).updateRating(eq(77L), any());
        verify(deliveryServiceClient).updateAgentRating(eq(88), anyMap());
    }

    @Test
    void flagReview_shouldPersistFlagReason() {
        Review existing = review(707, 907, 11, 77, 88, 4, 4, "Inappropriate", true, false);
        when(reviewRepository.findById(707)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlagReviewRequest request = new FlagReviewRequest();
        request.setReason("spam");
        ReviewResponse response = reviewService.flagReview(707, request);

        assertEquals(707, response.getReviewId());
        assertThat(response.getIsFlagged()).isTrue();
        assertEquals("spam", response.getFlagReason());
    }

    private Review review(Integer reviewId, Integer orderId, Integer customerId, Integer restaurantId, Integer agentId,
                          Integer foodRating, Integer deliveryRating, String comment, boolean verified, boolean flagged) {
        Review review = Review.builder()
                .reviewId(reviewId)
                .orderId(orderId)
                .customerId(customerId)
                .restaurantId(restaurantId)
                .agentId(agentId)
                .foodRating(foodRating)
                .deliveryRating(deliveryRating)
                .comment(comment)
                .reviewDate(LocalDate.of(2026, 5, 14))
                .isVerified(verified)
                .isFlagged(flagged)
                .createdAt(LocalDateTime.of(2026, 5, 14, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 14, 10, 5))
                .build();
        return review;
    }
}
