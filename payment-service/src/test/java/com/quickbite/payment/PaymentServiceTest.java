package com.quickbite.payment;

import com.quickbite.payment.dto.request.ProcessPaymentRequest;
import com.quickbite.payment.dto.request.RefundRequest;
import com.quickbite.payment.dto.request.WalletTopUpRequest;
import com.quickbite.payment.dto.response.PaymentResponse;
import com.quickbite.payment.dto.response.WalletResponse;
import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.entity.Wallet;
import com.quickbite.payment.exception.InsufficientBalanceException;
import com.quickbite.payment.exception.PaymentAlreadyRefundedException;
import com.quickbite.payment.notification.service.NotificationService;
import com.quickbite.payment.repository.PaymentRepository;
import com.quickbite.payment.repository.WalletRepository;
import com.quickbite.payment.repository.WalletStatementRepository;
import com.quickbite.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private WalletStatementRepository statementRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private ProcessPaymentRequest codRequest;
    private ProcessPaymentRequest walletRequest;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        codRequest = new ProcessPaymentRequest();
        codRequest.setOrderId(1L);
        codRequest.setCustomerId(10L);
        codRequest.setAmount(500.0);
        codRequest.setMode("COD");

        walletRequest = new ProcessPaymentRequest();
        walletRequest.setOrderId(2L);
        walletRequest.setCustomerId(10L);
        walletRequest.setAmount(300.0);
        walletRequest.setMode("WALLET");

        wallet = Wallet.builder()
                .walletId(1L)
                .customerId(10L)
                .balance(1000.0)
                .currency("INR")
                .build();
    }

    @Test
    void processPayment_COD_Success() {
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        Payment saved = Payment.builder()
                .paymentId(1L).orderId(1L).customerId(10L)
                .amount(500.0).mode("COD").status("PAID")
                .transactionId("QB-TXN-123").currency("INR").build();
        when(paymentRepository.save(any())).thenReturn(saved);

        PaymentResponse response = paymentService.processPayment(codRequest);

        assertNotNull(response);
        assertEquals("PAID", response.getStatus());
        assertEquals("COD", response.getMode());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void processPayment_InvalidMode_ThrowsException() {
        codRequest.setMode("INVALID");
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.processPayment(codRequest));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void payFromWallet_InsufficientBalance_ThrowsException() {
        wallet.setBalance(100.0); // less than 300.0 required
        when(walletRepository.findByCustomerId(10L)).thenReturn(Optional.of(wallet));

        assertThrows(InsufficientBalanceException.class,
                () -> paymentService.payFromWallet(walletRequest));
        verify(walletRepository, never()).debitBalance(any(), any());
    }

    @Test
    void payFromWallet_SufficientBalance_Success() {
        when(walletRepository.findByCustomerId(10L)).thenReturn(Optional.of(wallet));
        Payment saved = Payment.builder()
                .paymentId(2L).orderId(2L).customerId(10L)
                .amount(300.0).mode("WALLET").status("PAID")
                .transactionId("QB-TXN-456").currency("INR").build();
        when(paymentRepository.save(any())).thenReturn(saved);
        when(statementRepository.save(any())).thenReturn(null);

        PaymentResponse response = paymentService.payFromWallet(walletRequest);

        assertNotNull(response);
        assertEquals("PAID", response.getStatus());
        assertEquals("WALLET", response.getMode());
        verify(walletRepository).debitBalance(10L, 300.0);
    }

    @Test
    void refundPayment_AlreadyRefunded_ThrowsException() {
        Payment refunded = Payment.builder()
                .paymentId(1L).status("REFUNDED").build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(refunded));

        RefundRequest req = new RefundRequest();
        req.setPaymentId(1L);
        assertThrows(PaymentAlreadyRefundedException.class,
                () -> paymentService.refundPayment(1L, req));
    }

    @Test
    void addToWallet_Success() {
        when(walletRepository.findByCustomerId(10L)).thenReturn(Optional.of(wallet));
        Wallet updated = Wallet.builder().walletId(1L).customerId(10L)
                .balance(1500.0).currency("INR").build();
        when(walletRepository.findByCustomerId(10L))
                .thenReturn(Optional.of(wallet))
                .thenReturn(Optional.of(updated));
        when(statementRepository.save(any())).thenReturn(null);

        WalletTopUpRequest req = new WalletTopUpRequest();
        req.setCustomerId(10L);
        req.setAmount(500.0);
        req.setSourceMode("CARD");

        WalletResponse response = paymentService.addToWallet(req);
        assertNotNull(response);
        verify(walletRepository).creditBalance(10L, 500.0);
    }

    @Test
    void getWalletBalance_WalletNotExists_CreatesAndReturnsZero() {
        when(walletRepository.findByCustomerId(99L)).thenReturn(Optional.empty());
        Wallet newWallet = Wallet.builder().walletId(5L).customerId(99L).balance(0.0).build();
        when(walletRepository.save(any())).thenReturn(newWallet);

        Double balance = paymentService.getWalletBalance(99L);

        assertEquals(0.0, balance);
        verify(walletRepository).save(any(Wallet.class));
    }
}
