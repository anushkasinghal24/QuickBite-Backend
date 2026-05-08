package com.quickbite.payment.constants;

public class AppConstants {

    private AppConstants() {}

    // ===== PAYMENT STATUS =====
    public static final String PAYMENT_PENDING   = "PENDING";
    public static final String PAYMENT_PAID      = "PAID";
    public static final String PAYMENT_REFUNDED  = "REFUNDED";
    public static final String PAYMENT_FAILED    = "FAILED";

    // ===== PAYMENT MODE =====
    public static final String MODE_COD    = "COD";
    public static final String MODE_CARD   = "CARD";
    public static final String MODE_UPI    = "UPI";
    public static final String MODE_WALLET = "WALLET";

    // ===== WALLET STATEMENT TYPE =====
    public static final String STMT_CREDIT = "CREDIT";   // deposit / refund
    public static final String STMT_DEBIT  = "DEBIT";    // payment

    // ===== TRANSACTION ID PREFIX =====
    public static final String TXN_PREFIX  = "QB-TXN-";
    public static final String REF_PREFIX  = "QB-REF-";

    // ===== ROLES (must match auth-service) =====
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_ADMIN    = "ADMIN";

    // ===== CURRENCY =====
    public static final String CURRENCY_INR  = "INR";

    // ===== PAGINATION =====
    public static final int    DEFAULT_PAGE      = 0;
    public static final int    DEFAULT_PAGE_SIZE = 10;

    // ===== SUCCESS MESSAGES =====
    public static final String PAYMENT_SUCCESS        = "Payment processed successfully.";
    public static final String PAYMENT_REFUND_SUCCESS = "Refund initiated successfully.";
    public static final String WALLET_TOPUP_SUCCESS   = "Wallet topped up successfully.";
    public static final String WALLET_PAY_SUCCESS     = "Payment via wallet successful.";

    // ===== ERROR MESSAGES =====
    public static final String INSUFFICIENT_BALANCE   = "Insufficient wallet balance.";
    public static final String PAYMENT_NOT_FOUND      = "Payment record not found.";
    public static final String WALLET_NOT_FOUND       = "Wallet not found for this customer.";
    public static final String ALREADY_REFUNDED       = "Payment has already been refunded.";
    public static final String INVALID_PAYMENT_MODE   = "Invalid payment mode. Use COD/CARD/UPI/WALLET.";
}
