package com.quickbite.order.dto;

import com.quickbite.order.entity.Order.PaymentMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * PlaceOrderRequest
 *
 * Sent by customer at checkout to place an order.
 * Order-service will:
 *  1. Fetch cart snapshot from cart-service (via Feign)
 *  2. Validate restaurant is open (via restaurant-service Feign)
 *  3. Create Order + OrderItems
 *  4. Trigger payment processing (via payment-service Feign)
 *  5. Clear the customer's cart (via cart-service Feign)
 *  6. Assign a delivery agent (via delivery-service Feign)
 *  7. Send notification (via notification-service Feign)
 *
 * As per PDF Section 2.2 & 4.5
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceOrderRequest {

    /**
     * customerId â€” extracted from JWT in the controller.
     * Not sent by client. Set programmatically from token.
     */

    /**
     * Payment mode chosen at checkout.
     * COD | CARD | UPI | WALLET
     */
    @NotNull(message = "Payment mode is required")
    private PaymentMode modeOfPayment;

    /**
     * Full delivery address.
     * Customer can pick from saved addresses or type a new one.
     */
    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;

    /** Optional special instructions (e.g. "no onion", "call before delivery") */
    private String specialInstructions;
}
