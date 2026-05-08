package com.quickbite.order.dto;

import lombok.*;

/**
 * OrderItemResponse DTO
 * Represents a single item in an order response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private int orderItemId;
    private int menuItemId;
    private String name;
    private double price;
    private int quantity;
    private double lineTotal;
    private String customization;
    private boolean veg;
    private String imageUrl;
}
