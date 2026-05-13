package com.quickbite.review_service.serviceimpl;

import com.quickbite.restaurant.feign.DeliveryServiceClient;
import com.quickbite.restaurant.feign.NotificationServiceClient;
import com.quickbite.restaurant.service.RabbitNotificationPublisher;
import com.quickbite.restaurant.service.RestaurantService;
import com.quickbite.review_service.entity.Review;
import com.quickbite.review_service.feign.OrderServiceClient;
import com.quickbite.review_service.repository.ReviewRepository;
import com.quickbite.review_service.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    void getByRestaurant_shouldReturnMappedVerifiedReviews() {
        Review firstReview = Review.builder()
                .reviewId(1)
                .orderId(1001)
                .customerId(11)
                .restaurantId(77)
                .agentId(88)
                .foodRating(5)
                .deliveryRating(4)
                .comment("Great food")
                .reviewDate(LocalDate.of(2026, 5, 13))
                .isVerified(true)
                .isFlagged(false)
                .createdAt(LocalDateTime.of(2026, 5, 13, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 13, 10, 5))
                .build();

        Review secondReview = Review.builder()
                .reviewId(2)
                .orderId(1002)
                .customerId(12)
                .restaurantId(77)
                .agentId(88)
                .foodRating(4)
                .deliveryRating(5)
                .comment("Nice service")
                .reviewDate(LocalDate.of(2026, 5, 12))
                .isVerified(true)
                .isFlagged(false)
                .createdAt(LocalDateTime.of(2026, 5, 12, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 12, 9, 5))
                .build();

        when(reviewRepository.findTop10ByRestaurantIdAndIsVerifiedTrueOrderByCreatedAtDesc(77))
                .thenReturn(List.of(firstReview, secondReview));

        var responses = reviewService.getByRestaurant(77);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getReviewId()).isEqualTo(1);
        assertThat(responses.get(0).getRestaurantId()).isEqualTo(77);
        assertThat(responses.get(0).getComment()).isEqualTo("Great food");
        assertThat(responses.get(0).getIsVerified()).isTrue();
        assertThat(responses.get(1).getReviewId()).isEqualTo(2);
        assertThat(responses.get(1).getCustomerId()).isEqualTo(12);
        assertThat(responses.get(1).getFoodRating()).isEqualTo(4);

        verify(reviewRepository).findTop10ByRestaurantIdAndIsVerifiedTrueOrderByCreatedAtDesc(77);
    }

    @Test
    void getByCustomer_shouldReturnMappedReviewsForCustomer() {
        Review firstReview = Review.builder()
                .reviewId(3)
                .orderId(2001)
                .customerId(55)
                .restaurantId(99)
                .agentId(44)
                .foodRating(5)
                .deliveryRating(5)
                .comment("Excellent")
                .reviewDate(LocalDate.of(2026, 5, 11))
                .isVerified(true)
                .isFlagged(false)
                .createdAt(LocalDateTime.of(2026, 5, 11, 14, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 11, 14, 10))
                .build();

        Review secondReview = Review.builder()
                .reviewId(4)
                .orderId(2002)
                .customerId(55)
                .restaurantId(101)
                .agentId(null)
                .foodRating(4)
                .deliveryRating(null)
                .comment("Good overall")
                .reviewDate(LocalDate.of(2026, 5, 10))
                .isVerified(true)
                .isFlagged(false)
                .createdAt(LocalDateTime.of(2026, 5, 10, 15, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 10, 15, 5))
                .build();

        when(reviewRepository.findByCustomerId(55))
                .thenReturn(List.of(firstReview, secondReview));

        var responses = reviewService.getByCustomer(55);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getReviewId()).isEqualTo(3);
        assertThat(responses.get(0).getCustomerId()).isEqualTo(55);
        assertThat(responses.get(0).getRestaurantId()).isEqualTo(99);
        assertThat(responses.get(0).getDeliveryRating()).isEqualTo(5);
        assertThat(responses.get(1).getReviewId()).isEqualTo(4);
        assertThat(responses.get(1).getRestaurantId()).isEqualTo(101);
        assertThat(responses.get(1).getComment()).isEqualTo("Good overall");

        verify(reviewRepository).findByCustomerId(55);
    }
}
