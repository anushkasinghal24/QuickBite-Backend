package com.quickbite.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * RefundRequest
 * POST /api/v1/payments/{paymentId}/refund
 * Called by order-service when an order is cancelled.
 */
@Data
public class RefundRequest {

    @NotNull
    private Long paymentId;

    /** Reason for refund (for audit trail) */
    private String reason;

    /**
     * Refund destination:
     *   WALLET  â€” credited to wallet immediately
     *   ORIGINAL â€” credited to original payment mode (3-5 business days)
     * Default: WALLET (fastest for customer)
     */
    private String refundTo = "WALLET";
}
