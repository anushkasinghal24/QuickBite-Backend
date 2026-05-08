package com.quickbite.payment.service;

import com.quickbite.payment.dto.request.*;
import com.quickbite.payment.dto.response.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * PaymentService Interface â€” PDF Section 4.6
 *
 * Declares:
 *  processPayment, getByOrder, getByCustomer, refundPayment,
 *  getWalletBalance, addToWallet, payFromWallet,
 *  getWalletStatements, updatePaymentStatus
 *
 * Plus admin operations:
 *  getAllPayments, getRevenueSummary
 */
public interface PaymentService {

    // â”€â”€ Payment Operations â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Process payment when order is placed (called by order-service) */
    PaymentResponse processPayment(ProcessPaymentRequest request);

    /** Create a Razorpay checkout order for CARD/UPI payment flow. */
    RazorpayOrderResponse createRazorpayOrder(RazorpayCreateOrderRequest request);

    /** Create a Razorpay checkout order before the order is placed. */
    RazorpayOrderResponse createRazorpayCheckoutOrder(RazorpayCheckoutRequest request);

    /** Verify Razorpay checkout signature and finalize the payment. */
    PaymentResponse verifyRazorpayPayment(RazorpayVerifyPaymentRequest request);

    /** Verify a pre-order Razorpay checkout signature. */
    void verifyRazorpayCheckoutPayment(RazorpayCheckoutVerifyRequest request);

    /** Create a Razorpay order for wallet top-up. */
    RazorpayWalletTopUpResponse createRazorpayWalletTopUpOrder(RazorpayWalletTopUpCreateRequest request);

    /** Verify Razorpay wallet top-up payment signature. */
    void verifyRazorpayWalletTopUpPayment(RazorpayWalletTopUpVerifyRequest request);

    /** Get payment by orderId (order-service + customer use this) */
    PaymentResponse getByOrderId(Long orderId);

    /** Get all payments for a customer */
    PagedResponse<PaymentResponse> getByCustomerId(Long customerId, Pageable pageable);

    /** Get payment by paymentId */
    PaymentResponse getByPaymentId(Long paymentId);

    /**
     * Initiate refund on order cancellation.
     * refundTo=WALLET â†’ credited immediately to wallet.
     * refundTo=ORIGINAL â†’ marks as refunded (external gateway handles 3-5 days).
     * Called by order-service on cancellation.
     */
    PaymentResponse refundPayment(Long paymentId, RefundRequest request);

    /** Admin: get all payments paginated */
    PagedResponse<PaymentResponse> getAllPayments(Pageable pageable);

    /** Admin: total revenue between dates */
    Double getRevenueBetween(String startDate, String endDate);

    // â”€â”€ Wallet Operations â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Get or auto-create wallet for a customer */
    WalletResponse getOrCreateWallet(Long customerId);

    /** Get current wallet balance */
    Double getWalletBalance(Long customerId);

    /** Add money to wallet (deposit via CARD/UPI) */
    WalletResponse addToWallet(WalletTopUpRequest request);

    /**
     * Debit wallet for order payment.
     * Validates balance >= amount BEFORE debiting (PDF: no negative balance).
     * Called internally when mode=WALLET in processPayment.
     */
    PaymentResponse payFromWallet(ProcessPaymentRequest request);

    /** Get wallet transaction statement history (paginated) */
    PagedResponse<WalletStatementResponse> getWalletStatements(Long customerId, Pageable pageable);

    /** Update payment status (internal use) */
    void updatePaymentStatus(Long paymentId, String status);
}
