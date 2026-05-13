package com.quickbite.order.cart.dto;

import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuantityRequest {

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}
