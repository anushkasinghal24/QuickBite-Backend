package com.quickbite.review_service.serviceimpl;

import com.quickbite.restaurant.feign.DeliveryServiceClient;
import com.quickbite.restaurant.feign.NotificationServiceClient;
import com.quickbite.restaurant.service.RabbitNotificationPublisher;
import com.quickbite.restaurant.service.RestaurantService;
import com.quickbite.review_service.dto.request.AddReviewRequest;
import com.quickbite.review_service.dto.response.ApiResponse;
import com.quickbite.review_service.dto.response.OrderDetailsDTO;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
}
