package com.quickbite.order.dto;

import lombok.*;

/**
 * TopSellingItemDTO
 *
 * Represents one menu item in the earnings analytics response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopSellingItemDTO {

    private int menuItemId;
    private String itemName;
    private long quantitySold;
    private double revenue;
}
