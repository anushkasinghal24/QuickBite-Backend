package com.quickbite.payment.service.impl;

import com.quickbite.payment.constants.AppConstants;
import com.quickbite.payment.dto.request.*;
import com.quickbite.payment.dto.response.*;
import com.quickbite.payment.entity.*;
import com.quickbite.payment.exception.*;
import com.quickbite.payment.repository.*;
import com.quickbite.payment.service.PaymentService;
import com.quickbite.payment.notification.dto.request.SendNotificationRequest;
import com.quickbite.payment.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PaymentServiceImpl â€” complete implementation of PaymentService.
 *
 * Key business rules (PDF Section 2.6):
 * 1. Wallet balance NEVER goes below 0 â€” validated before debit.
 * 2. Cart-to-order payment is ATOMIC â€” @Transactional prevents partial states.
 * 3. Refunds go to WALLET (instant) or ORIGINAL mode (3-5 days).
 * 4. Every wallet operation generates a WalletStatement for audit trail.
 * 5. COD payments are created as PAID immediately (no gateway needed).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository      paymentRepository;
    private final WalletRepository       walletRepository;
    private final WalletStatementRepository statementRepository;
    private final NotificationService    notificationService;

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // PROCESS PAYMENT â€” entry point called by order-service
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        log.info("Processing payment: orderId={}, mode={}, amount={}",
                request.getOrderId(), request.getMode(), request.getAmount());

        validatePaymentMode(request.getMode());

        // Prevent duplicate payment for same order
        if (paymentRepository.existsByOrderId(request.getOrderId())) {
            return mapToResponse(paymentRepository.findByOrderId(request.getOrderId()).get());
        }

        // WALLET payments have extra flow
        if (AppConstants.MODE_WALLET.equalsIgnoreCase(request.getMode())) {
            return payFromWallet(request);
        }

        // COD / CARD / UPI
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .mode(request.getMode().toUpperCase())
                .currency(request.getCurrency() != null ? request.getCurrency() : AppConstants.CURRENCY_INR)
                .transactionId(generateTransactionId())
                .build();

        if (AppConstants.MODE_COD.equalsIgnoreCase(request.getMode())) {
            // COD: mark as PAID immediately (actual cash collected at delivery)
            payment.setStatus(AppConstants.PAYMENT_PAID);
            payment.setPaidAt(LocalDateTime.now());
        } else {
            // CARD / UPI: In production, call Razorpay/Stripe here.
            // For now, simulate success.
            payment.setStatus(AppConstants.PAYMENT_PAID);
            payment.setPaidAt(LocalDateTime.now());
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Payment saved: id={}, status={}", saved.getPaymentId(), saved.getStatus());

        // Notify customer (payment receipt) â€” async, graceful fallback
        sendPaymentNotification(saved, "PAYMENT_RECEIPT");

        return mapToResponse(saved);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // PAY FROM WALLET â€” debit wallet balance atomically
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    public PaymentResponse payFromWallet(ProcessPaymentRequest request) {
        Wallet wallet = getOrCreateWalletEntity(request.getCustomerId());

        // PDF Section 2.6: validate sufficient balance BEFORE debiting
        if (wallet.getBalance() < request.getAmount()) {
            throw new InsufficientBalanceException(
                String.format("Insufficient wallet balance. Available: â‚¹%.2f, Required: â‚¹%.2f",
                        wallet.getBalance(), request.getAmount()));
        }

        // Atomic debit
        walletRepository.debitBalance(request.getCustomerId(), request.getAmount());
        double closingBalance = wallet.getBalance() - request.getAmount();

        // Statement record
        WalletStatement stmt = WalletStatement.builder()
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .type(AppConstants.STMT_DEBIT)
                .description("Payment for Order #" + request.getOrderId())
                .closingBalance(closingBalance)
                .referenceId("ORDER-" + request.getOrderId())
                .wallet(wallet)
                .build();
        statementRepository.save(stmt);

        // Create payment record
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .mode(AppConstants.MODE_WALLET)
                .currency(AppConstants.CURRENCY_INR)
                .transactionId(generateTransactionId())
                .status(AppConstants.PAYMENT_PAID)
                .paidAt(LocalDateTime.now())
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Wallet payment: customerId={}, amount={}, balance after={}",
                request.getCustomerId(), request.getAmount(), closingBalance);

        sendPaymentNotification(saved, "PAYMENT_RECEIPT");
        return mapToResponse(saved);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // REFUND â€” called by order-service on cancellation
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    public PaymentResponse refundPayment(Long paymentId, RefundRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found: " + paymentId));

        if (AppConstants.PAYMENT_REFUNDED.equals(payment.getStatus())) {
            throw new PaymentAlreadyRefundedException(AppConstants.ALREADY_REFUNDED);
        }
        if (!AppConstants.PAYMENT_PAID.equals(payment.getStatus())) {
            throw new IllegalArgumentException("Only PAID payments can be refunded.");
        }

        String refundTxnId = generateRefundId();
        String refundTo = request.getRefundTo() != null ? request.getRefundTo() : "WALLET";

        // Credit back to wallet (instant) regardless of original mode when refundTo=WALLET
        if ("WALLET".equalsIgnoreCase(refundTo) || AppConstants.MODE_WALLET.equals(payment.getMode())) {
            walletRepository.creditBalance(payment.getCustomerId(), payment.getAmount());

            Wallet wallet = getOrCreateWalletEntity(payment.getCustomerId());
            double closingBalance = wallet.getBalance() + payment.getAmount();

            WalletStatement stmt = WalletStatement.builder()
                    .customerId(payment.getCustomerId())
                    .amount(payment.getAmount())
                    .type(AppConstants.STMT_CREDIT)
                    .description("Refund for Order #" + payment.getOrderId()
                            + (request.getReason() != null ? " â€” " + request.getReason() : ""))
                    .closingBalance(closingBalance)
                    .referenceId("REFUND-" + paymentId)
                    .wallet(wallet)
                    .build();
            statementRepository.save(stmt);
            log.info("Refund credited to wallet: customerId={}, amount={}",
                    payment.getCustomerId(), payment.getAmount());
        }
        // ORIGINAL mode â€” just mark as refunded, external gateway handles 3-5 days
        // (Production: call Razorpay refund API here)

        paymentRepository.updateToRefunded(
                paymentId,
                AppConstants.PAYMENT_REFUNDED,
                LocalDateTime.now(),
                refundTxnId);

        // Re-fetch updated
        payment.setStatus(AppConstants.PAYMENT_REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        payment.setRefundTransactionId(refundTxnId);

        log.info("Refund processed: paymentId={}, refundTxn={}", paymentId, refundTxnId);
        sendRefundNotification(payment);

        return mapToResponse(payment);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // GET OPERATIONS
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No payment found for orderId: " + orderId));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getByCustomerId(Long customerId, Pageable pageable) {
        Page<PaymentResponse> page = paymentRepository
                .findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::mapToResponse);
        return PagedResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getByPaymentId(Long paymentId) {
        return mapToResponse(paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found: " + paymentId)));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getAllPayments(Pageable pageable) {
        Page<PaymentResponse> page = paymentRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToResponse);
        return PagedResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getRevenueBetween(String startDate, String endDate) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime start = LocalDate.parse(startDate, fmt).atStartOfDay();
        LocalDateTime end   = LocalDate.parse(endDate,   fmt).atTime(23, 59, 59);
        Double revenue = paymentRepository.sumRevenueBetween(start, end);
        return revenue != null ? revenue : 0.0;
    }

    @Override
    public void updatePaymentStatus(Long paymentId, String status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        payment.setStatus(status);
        paymentRepository.save(payment);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // WALLET OPERATIONS
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getOrCreateWallet(Long customerId) {
        Wallet wallet = getOrCreateWalletEntity(customerId);
        return mapWalletToResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getWalletBalance(Long customerId) {
        Wallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> createNewWallet(customerId));
        return wallet.getBalance();
    }

    @Override
    public WalletResponse addToWallet(WalletTopUpRequest request) {
        Wallet wallet = getOrCreateWalletEntity(request.getCustomerId());

        walletRepository.creditBalance(request.getCustomerId(), request.getAmount());
        double newBalance = wallet.getBalance() + request.getAmount();

        WalletStatement stmt = WalletStatement.builder()
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .type(AppConstants.STMT_CREDIT)
                .description("Wallet top-up via " + request.getSourceMode())
                .closingBalance(newBalance)
                .referenceId(request.getGatewayTransactionId() != null
                        ? request.getGatewayTransactionId()
                        : generateTransactionId())
                .wallet(wallet)
                .build();
        statementRepository.save(stmt);

        log.info("Wallet topped up: customerId={}, amount={}, newBalance={}",
                request.getCustomerId(), request.getAmount(), newBalance);

        // Notify customer
        sendWalletTopUpNotification(request.getCustomerId(), request.getAmount(), newBalance);

        // Re-fetch fresh balance
        return mapWalletToResponse(walletRepository.findByCustomerId(request.getCustomerId()).get());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<WalletStatementResponse> getWalletStatements(Long customerId, Pageable pageable) {
        Page<WalletStatementResponse> page = statementRepository
                .findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::mapStatementToResponse);
        return PagedResponse.of(page);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // PRIVATE HELPERS
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    private Wallet getOrCreateWalletEntity(Long customerId) {
        return walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> createNewWallet(customerId));
    }

    private Wallet createNewWallet(Long customerId) {
        log.info("Creating new wallet for customerId={}", customerId);
        return walletRepository.save(Wallet.builder()
                .customerId(customerId)
                .balance(0.0)
                .currency(AppConstants.CURRENCY_INR)
                .build());
    }

    private String generateTransactionId() {
        return AppConstants.TXN_PREFIX + UUID.randomUUID().toString().toUpperCase().replace("-", "");
    }

    private String generateRefundId() {
        return AppConstants.REF_PREFIX + UUID.randomUUID().toString().toUpperCase().replace("-", "");
    }

    private void validatePaymentMode(String mode) {
        if (!List.of("COD","CARD","UPI","WALLET").contains(mode.toUpperCase())) {
            throw new IllegalArgumentException(AppConstants.INVALID_PAYMENT_MODE);
        }
    }

    // â”€â”€ Notification Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void sendPaymentNotification(Payment payment, String type) {
        try {
            SendNotificationRequest request = new SendNotificationRequest();
            request.setRecipientId(payment.getCustomerId());
            request.setType(type);
            request.setTitle("Payment Successful!");
            request.setMessage(String.format("â‚¹%.2f paid via %s for Order #%d",
                    payment.getAmount(), payment.getMode(), payment.getOrderId()));
            request.setRelatedId(payment.getOrderId());
            request.setRelatedType("ORDER");
            request.setChannel("APP");
            notificationService.send(request);
        } catch (Exception e) {
            log.warn("Failed to send payment notification: {}", e.getMessage());
        }
    }

    private void sendRefundNotification(Payment payment) {
        try {
            SendNotificationRequest request = new SendNotificationRequest();
            request.setRecipientId(payment.getCustomerId());
            request.setType("REFUND_INITIATED");
            request.setTitle("Refund Initiated");
            request.setMessage(String.format("â‚¹%.2f refund initiated for Order #%d",
                    payment.getAmount(), payment.getOrderId()));
            request.setRelatedId(payment.getOrderId());
            request.setChannel("APP");
            notificationService.send(request);
        } catch (Exception e) {
            log.warn("Failed to send refund notification: {}", e.getMessage());
        }
    }

    private void sendWalletTopUpNotification(Long customerId, Double amount, Double newBalance) {
        try {
            SendNotificationRequest request = new SendNotificationRequest();
            request.setRecipientId(customerId);
            request.setType("WALLET_TOPUP");
            request.setTitle("Wallet Topped Up");
            request.setMessage(String.format("â‚¹%.2f added. New balance: â‚¹%.2f", amount, newBalance));
            request.setChannel("APP");
            notificationService.send(request);
        } catch (Exception e) {
            log.warn("Failed to send wallet top-up notification: {}", e.getMessage());
        }
    }

    // â”€â”€ Mappers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private PaymentResponse mapToResponse(Payment p) {
        return PaymentResponse.builder()
                .paymentId(p.getPaymentId())
                .orderId(p.getOrderId())
                .customerId(p.getCustomerId())
                .amount(p.getAmount())
                .status(p.getStatus())
                .mode(p.getMode())
                .transactionId(p.getTransactionId())
                .currency(p.getCurrency())
                .refundTransactionId(p.getRefundTransactionId())
                .failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt())
                .paidAt(p.getPaidAt())
                .refundedAt(p.getRefundedAt())
                .build();
    }

    private WalletResponse mapWalletToResponse(Wallet w) {
        return WalletResponse.builder()
                .walletId(w.getWalletId())
                .customerId(w.getCustomerId())
                .balance(w.getBalance())
                .currency(w.getCurrency())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }

    private WalletStatementResponse mapStatementToResponse(WalletStatement s) {
        return WalletStatementResponse.builder()
                .statementId(s.getStatementId())
                .customerId(s.getCustomerId())
                .amount(s.getAmount())
                .type(s.getType())
                .description(s.getDescription())
                .closingBalance(s.getClosingBalance())
                .referenceId(s.getReferenceId())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
