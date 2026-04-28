package com.quickbite.restaurant.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/** PUT /api/v1/restaurants/{id} — owner updates their restaurant profile */
@Data
public class UpdateRestaurantRequest {

    @Size(min = 2, max = 150)
    private String name;

    @Size(max = 500)
    private String description;

    private String cuisine;
    private String address;
    private String city;
    private String state;
    private String pincode;

    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double longitude;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter valid 10-digit mobile number")
    private String phone;

    @Email
    private String email;

    private String imageUrl;

    @Min(1) private Double deliveryRadius;
    @Min(0) private Double minOrderAmount;

    @Min(10) @Max(120)
    private Integer estimatedDeliveryMin;

    private String openingTime;
    private String closingTime;
}
