package com.quickbite.delivery_service.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for live GPS location update.
 * PDF Section 2.4: "Update live GPS coordinates at configurable intervals
 *                   for real-time tracking by customers."
 * PDF NFR: "Delivery agent location updates pushed via WebSocket every 15 seconds."
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLocationRequest {

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0",  message = "Latitude must be <= 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0",  message = "Longitude must be <= 180")
    private Double longitude;
}
