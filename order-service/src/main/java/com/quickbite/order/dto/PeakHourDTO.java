package com.quickbite.order.dto;

import lombok.*;

/**
 * PeakHourDTO
 *
 * Represents a busy hour bucket in the earnings analytics response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeakHourDTO {

    private int hour;
    private String label;
    private long orderCount;
}
