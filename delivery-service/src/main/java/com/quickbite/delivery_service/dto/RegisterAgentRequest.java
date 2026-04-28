package com.quickbite.delivery_service.dto;

import com.quickbite.delivery_service.entity.DeliveryAgent;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for agent registration.
 * PDF Section 2.4: "Register as a delivery agent with personal details,
 *                   vehicle type, and vehicle registration number."
 *
 * userId is extracted from JWT token — not sent in request body.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterAgentRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name max 100 chars")
    private String fullName;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number (10 digits, starts with 6-9)")
    private String phone;

    @NotNull(message = "Vehicle type is required (BIKE/CYCLE/SCOOTER/CAR)")
    private DeliveryAgent.VehicleType vehicleType;

    @NotBlank(message = "Vehicle number is required")
    @Size(max = 20, message = "Vehicle number max 20 chars")
    private String vehicleNumber;
}
