package com.quickbite.payment.service.impl;

import com.quickbite.payment.dto.request.ProcessPaymentRequest;
import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.entity.Wallet;
import com.quickbite.payment.exception.InsufficientBalanceException;
import com.quickbite.payment.notification.service.NotificationService;
import com.quickbite.payment.repository.PaymentRepository;
import com.quickbite.payment.repository.WalletRepository;
import com.quickbite.payment.repository.WalletStatementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletStatementRepository statementRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private ProcessPaymentRequest codRequest;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        codRequest = new ProcessPaymentRequest();
        codRequest.setOrderId(101L);
        codRequest.setCustomerId(35L);
        codRequest.setAmount(499.0);
        codRequest.setMode("COD");
        codRequest.setCurrency("INR");

        wallet = Wallet.builder()
                .walletId(1L)
                .customerId(35L)
                .balance(100.0)
                .currency("INR")
                .build();
    }

    @Test
    void processPayment_shouldCreateCodPayment() {
        when(paymentRepository.existsByOrderId(101L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setPaymentId(1L);
            return payment;
        });

        var response = paymentService.processPayment(codRequest);

        assertNotNull(response);
        assertEquals(101L, response.getOrderId());
        assertEquals("PAID", response.getStatus());
        assertEquals("COD", response.getMode());
        verify(paymentRepository).save(any(Payment.class));
        verify(notificationService).send(any());
    }

    @Test
    void payFromWallet_shouldThrowWhenBalanceIsLow() {
        ProcessPaymentRequest walletRequest = new ProcessPaymentRequest();
        walletRequest.setOrderId(102L);
        walletRequest.setCustomerId(35L);
        walletRequest.setAmount(300.0);
        walletRequest.setMode("WALLET");
        walletRequest.setCurrency("INR");

        when(walletRepository.findByCustomerId(35L)).thenReturn(Optional.of(wallet));

        assertThrows(InsufficientBalanceException.class,
                () -> paymentService.payFromWallet(walletRequest));

        verify(walletRepository, never()).debitBalance(any(), any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
