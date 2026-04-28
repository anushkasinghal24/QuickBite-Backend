package com.quickbite.review_service.dto.response;

import lombok.*;

/** Average rating response — for restaurant or agent */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RatingAverageResponse {
    private Integer entityId;
    private String  entityType;   // "RESTAURANT" or "AGENT"
    private Double  avgRating;
    private Long    totalReviews;
}
