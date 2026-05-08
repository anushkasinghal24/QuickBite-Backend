package com.quickbite.delivery_service.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * PDF Section 2.4: "Toggle availability (online/offline) to start or stop receiving orders."
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SetAvailabilityRequest {

    @NotNull(message = "Available flag is required (true=online, false=offline)")
    private Boolean available;
}
