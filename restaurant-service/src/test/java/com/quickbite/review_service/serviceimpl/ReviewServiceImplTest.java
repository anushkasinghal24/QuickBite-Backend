package com.quickbite.review_service.serviceimpl;

import com.quickbite.restaurant.feign.DeliveryServiceClient;
import com.quickbite.restaurant.feign.NotificationServiceClient;
import com.quickbite.restaurant.service.RabbitNotificationPublisher;
import com.quickbite.restaurant.service.RestaurantService;
import com.quickbite.restaurant.dto.request.UpdateRatingRequest;
import com.quickbite.review_service.dto.request.AddReviewRequest;
import com.quickbite.review_service.dto.request.FlagReviewRequest;
import com.quickbite.review_service.dto.request.UpdateReviewRequest;
import com.quickbite.review_service.dto.response.ApiResponse;
import com.quickbite.review_service.dto.response.OrderDetailsDTO;
import com.quickbite.review_service.dto.response.RatingAverageResponse;
import com.quickbite.review_service.dto.response.ReviewResponse;
import com.quickbite.review_service.entity.Review;
import com.quickbite.review_service.exception.DuplicateReviewException;
import com.quickbite.review_service.repository.ReviewRepository;
import com.quickbite.review_service.service.ReviewService;
import com.quickbite.review_service.feign.OrderServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private RestaurantService restaurantService;

    @Mock
    private DeliveryServiceClient deliveryServiceClient;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @Mock
    private RabbitNotificationPublisher rabbitNotificationPublisher;

    @Mock
    private ReviewService self;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    void addReview_shouldPersistAndFallbackToFeignWhenRabbitIsUnavailable() {
        LocalDate today = LocalDate.now();

        AddReviewRequest request = AddReviewRequest.builder()
                .orderId(1001)
                .restaurantId(77)
                .foodRating(5)
                .deliveryRating(4)
                .comment("Excellent food")
                .build();

        OrderDetailsDTO order = OrderDetailsDTO.builder()
                .orderId(1001)
                .customerId(11)
                .restaurantId(77)
                .deliveryAgentId(88)
                .orderStatus("DELIVERED")
                .build();

        Review savedReview = Review.builder()
                .reviewId(501)
                .orderId(1001)
                .customerId(11)
                .restaurantId(77)
                .agentId(88)
                .foodRating(5)
                .deliveryRating(4)
                .comment("Excellent food")
                .reviewDate(today)
                .isVerified(true)
                .isFlagged(false)
                .build();

        when(orderServiceClient.getOrderById(1001))
                .thenReturn(ApiResponse.success("ok", order));
        when(reviewRepository.existsByOrderId(1001)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(self.getAvgFoodRating(77)).thenReturn(4.5);
        when(reviewRepository.countByRestaurantId(77)).thenReturn(1L);
        when(self.getAvgDeliveryRating(88)).thenReturn(4.0);
        when(rabbitNotificationPublisher.publish(anyMap())).thenReturn(false);

        ReviewResponse response = reviewService.addReview(11, request);

        assertThat(response.getReviewId()).isEqualTo(501);
        assertThat(response.getOrderId()).isEqualTo(1001);
        assertThat(response.getRestaurantId()).isEqualTo(77);
        assertThat(response.getAgentId()).isEqualTo(88);
        assertThat(response.getFoodRating()).isEqualTo(5);
        assertThat(response.getReviewDate()).isEqualTo(today);

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        assertThat(reviewCaptor.getValue().getCustomerId()).isEqualTo(11);
        assertThat(reviewCaptor.getValue().getRestaurantId()).isEqualTo(77);
        assertThat(reviewCaptor.getValue().getAgentId()).isEqualTo(88);
        assertThat(reviewCaptor.getValue().getIsVerified()).isTrue();
        assertThat(reviewCaptor.getValue().getIsFlagged()).isFalse();

        verify(restaurantService).updateRating(eq(77L), any(UpdateRatingRequest.class));
        verify(deliveryServiceClient).updateAgentRating(eq(88), anyMap());
        verify(notificationServiceClient).sendNotification(anyMap());
    }

    @Test
    void addReview_shouldRejectDuplicateReviewForSameOrder() {
        AddReviewRequest request = AddReviewRequest.builder()
                .orderId(1001)
                .restaurantId(77)
                .foodRating(4)
                .build();

        OrderDetailsDTO order = OrderDetailsDTO.builder()
                .orderId(1001)
                .customerId(11)
                .restaurantId(77)
                .deliveryAgentId(null)
                .orderStatus("DELIVERED")
                .build();

        when(orderServiceClient.getOrderById(1001))
                .thenReturn(ApiResponse.success("ok", order));
        when(reviewRepository.existsByOrderId(1001)).thenReturn(true);

        assertThrows(DuplicateReviewException.class,
                () -> reviewService.addReview(11, request));

        verify(reviewRepository, never()).save(any(Review.class));
        verify(restaurantService, never()).updateRating(eq(77L), any(UpdateRatingRequest.class));
        verify(notificationServiceClient, never()).sendNotification(anyMap());
    }

    @Test
    void getRestaurantRatingSummary_shouldReturnRoundedAverageAndCount() {
        when(self.getAvgFoodRating(77)).thenReturn(4.556);
        when(reviewRepository.countByRestaurantId(77)).thenReturn(8L);

        RatingAverageResponse response = reviewService.getRestaurantRatingSummary(77);

        assertThat(response.getEntityId()).isEqualTo(77);
        assertThat(response.getEntityType()).isEqualTo("RESTAURANT");
        assertThat(response.getAvgRating()).isEqualTo(4.556);
        assertThat(response.getTotalReviews()).isEqualTo(8L);
    }

    @Test
    void getAgentRatingSummary_shouldReturnRoundedAverageAndCount() {
        when(self.getAvgDeliveryRating(88)).thenReturn(4.25);
        when(reviewRepository.countByAgentId(88)).thenReturn(3L);

        RatingAverageResponse response = reviewService.getAgentRatingSummary(88);

        assertThat(response.getEntityId()).isEqualTo(88);
        assertThat(response.getEntityType()).isEqualTo("AGENT");
        assertThat(response.getAvgRating()).isEqualTo(4.25);
        assertThat(response.getTotalReviews()).isEqualTo(3L);
    }

    @Test
    void flagReview_shouldMarkReviewFlaggedAndPersistChanges() {
        Review review = Review.builder()
                .reviewId(9)
                .orderId(1009)
                .customerId(11)
                .restaurantId(77)
                .foodRating(3)
                .isVerified(true)
                .isFlagged(false)
                .build();

        when(reviewRepository.findById(9)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.flagReview(9, new FlagReviewRequest("Spam content"));

        assertThat(response.getReviewId()).isEqualTo(9);
        assertThat(response.getIsFlagged()).isTrue();
        assertThat(response.getFlagReason()).isEqualTo("Spam content");
        assertThat(review.getIsVerified()).isTrue();

        verify(reviewRepository).save(review);
    }

    @Test
    void verifyReview_shouldClearFlagAndRepushRatings() {
        Review review = Review.builder()
                .reviewId(10)
                .orderId(1010)
                .customerId(11)
                .restaurantId(77)
                .agentId(88)
                .foodRating(5)
                .deliveryRating(4)
                .isVerified(false)
                .isFlagged(true)
                .flagReason("Needs review")
                .build();

        when(reviewRepository.findById(10)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(self.getAvgFoodRating(77)).thenReturn(4.8);
        when(reviewRepository.countByRestaurantId(77)).thenReturn(2L);
        when(self.getAvgDeliveryRating(88)).thenReturn(4.0);

        ReviewResponse response = reviewService.verifyReview(10);

        assertThat(response.getReviewId()).isEqualTo(10);
        assertThat(response.getIsVerified()).isTrue();
        assertThat(response.getIsFlagged()).isFalse();
        assertThat(response.getFlagReason()).isNull();

        verify(restaurantService).updateRating(eq(77L), any(UpdateRatingRequest.class));
        verify(deliveryServiceClient).updateAgentRating(eq(88), anyMap());
    }

    @Test
    void updateReview_shouldApplyOwnerChangesAndRepushRatings() {
        Review review = Review.builder()
                .reviewId(20)
                .orderId(1020)
                .customerId(11)
                .restaurantId(77)
                .agentId(88)
                .foodRating(3)
                .deliveryRating(3)
                .comment("Old comment")
                .isVerified(false)
                .isFlagged(true)
                .flagReason("Pending moderation")
                .build();

        UpdateReviewRequest request = UpdateReviewRequest.builder()
                .foodRating(5)
                .comment("Updated comment")
                .build();

        when(reviewRepository.findById(20)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(self.getAvgFoodRating(77)).thenReturn(4.7);
        when(reviewRepository.countByRestaurantId(77)).thenReturn(4L);
        when(self.getAvgDeliveryRating(88)).thenReturn(4.2);

        ReviewResponse response = reviewService.updateReview(20, 11, request);

        assertThat(response.getReviewId()).isEqualTo(20);
        assertThat(response.getFoodRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo("Updated comment");
        assertThat(response.getIsVerified()).isTrue();
        assertThat(response.getIsFlagged()).isFalse();
        assertThat(response.getFlagReason()).isNull();

        verify(restaurantService).updateRating(eq(77L), any(UpdateRatingRequest.class));
        verify(deliveryServiceClient).updateAgentRating(eq(88), anyMap());
    }

    @Test
    void deleteReview_shouldRemoveReviewAndRepushRatingsForAdmin() {
        Review review = Review.builder()
                .reviewId(30)
                .orderId(1030)
                .customerId(11)
                .restaurantId(77)
                .agentId(88)
                .foodRating(4)
                .deliveryRating(4)
                .build();

        when(reviewRepository.findById(30)).thenReturn(Optional.of(review));
        when(self.getAvgFoodRating(77)).thenReturn(4.4);
        when(reviewRepository.countByRestaurantId(77)).thenReturn(1L);
        when(self.getAvgDeliveryRating(88)).thenReturn(4.1);

        reviewService.deleteReview(30, 999, "ADMIN");

        verify(reviewRepository).delete(review);
        verify(restaurantService).updateRating(eq(77L), any(UpdateRatingRequest.class));
        verify(deliveryServiceClient).updateAgentRating(eq(88), anyMap());
    }
}
