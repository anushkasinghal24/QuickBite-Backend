package com.quickbite.restaurant.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * RegisterRestaurantRequest
 * POST /api/v1/restaurants
 * Used by ROLE_OWNER to register a new restaurant.
 */
@Data
public class RegisterRestaurantRequest {

    @NotBlank(message = "Restaurant name is required")
    @Size(min = 2, max = 150)
    private String name;

    @Size(max = 500)
    private String description;

    @NotBlank(message = "Cuisine type is required")
    private String cuisine;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    private String state;
    private String pincode;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter valid 10-digit Indian mobile number")
    private String phone;

    @Email
    private String email;

    private String imageUrl;

    @Min(value = 1)
    private Double deliveryRadius = 5.0;

    @Min(value = 0)
    private Double minOrderAmount = 0.0;

    @Min(value = 0)
    private Double costForTwo = 0.0;

    @Min(value = 10) @Max(value = 120)
    private Integer estimatedDeliveryMin = 30;

    private String openingTime;  // "09:00"
    private String closingTime;  // "23:00"
}
