package com.quickbite.payment.service.impl;

import com.quickbite.payment.config.RazorpayProperties;
import com.quickbite.payment.dto.ApiResponse;
import com.quickbite.payment.dto.UserContactDTO;
import com.quickbite.payment.dto.request.*;
import com.quickbite.payment.dto.response.PaymentResponse;
import com.quickbite.payment.dto.response.RazorpayOrderResponse;
import com.quickbite.payment.notification.dto.response.NotificationResponse;
import com.quickbite.payment.dto.response.WalletResponse;
import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.entity.Wallet;
import com.quickbite.payment.exception.InsufficientBalanceException;
import com.quickbite.payment.exception.PaymentAlreadyRefundedException;
import com.quickbite.payment.feign.AuthServiceClient;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplCoverageTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private WalletStatementRepository statementRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private RazorpayProperties razorpayProperties;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Wallet wallet;
    private Payment paidPayment;
    private Payment refundedPayment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "restTemplate", restTemplate);

        lenient().when(razorpayProperties.getKeyId()).thenReturn("rzp_test_key");
        lenient().when(razorpayProperties.getKeySecret()).thenReturn("rzp_test_secret");
        lenient().when(razorpayProperties.getApiBaseUrl()).thenReturn("https://api.razorpay.com");

        wallet = Wallet.builder()
                .walletId(1L)
                .customerId(35L)
                .balance(1000.0)
                .currency("INR")
                .build();

        paidPayment = Payment.builder()
                .paymentId(101L)
                .orderId(500L)
                .customerId(35L)
                .amount(250.0)
                .mode("CARD")
                .currency("INR")
                .transactionId("QB-TXN-123")
                .status("PAID")
                .build();

        refundedPayment = Payment.builder()
                .paymentId(102L)
                .orderId(501L)
                .customerId(35L)
                .amount(120.0)
                .mode("CARD")
                .currency("INR")
                .transactionId("QB-TXN-456")
                .status("REFUNDED")
                .build();

        UserContactDTO contact = new UserContactDTO();
        contact.setUserId(35);
        contact.setEmail("customer@quickbite.com");
        ApiResponse<UserContactDTO> contactResponse = ApiResponse.<UserContactDTO>builder()
                .success(true)
                .message("ok")
                .data(contact)
                .build();
        lenient().when(authServiceClient.getUserById(35)).thenReturn(contactResponse);
        lenient().when(notificationService.send(any())).thenReturn(NotificationResponse.builder().build());
    }

    @Test
    void processPayment_shouldReturnExistingPaymentWhenOrderAlreadyPaid() {
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setOrderId(500L);
        request.setCustomerId(35L);
        request.setAmount(250.0);
        request.setMode("CARD");
        request.setCurrency("INR");

        when(paymentRepository.existsByOrderId(500L)).thenReturn(true);
        when(paymentRepository.findByOrderId(500L)).thenReturn(Optional.of(paidPayment));

        PaymentResponse response = paymentService.processPayment(request);

        assertEquals(500L, response.getOrderId());
        assertEquals("PAID", response.getStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processPayment_shouldRejectInvalidMode() {
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setOrderId(501L);
        request.setCustomerId(35L);
        request.setAmount(250.0);
        request.setMode("CASH");
        request.setCurrency("INR");

        assertThrows(IllegalArgumentException.class, () -> paymentService.processPayment(request));
    }

    @Test
    void createRazorpayOrder_shouldCreateGatewayOrder() {
        RazorpayCreateOrderRequest request = new RazorpayCreateOrderRequest();
        request.setOrderId(700L);
        request.setCustomerId(35L);
        request.setAmount(199.0);
        request.setMode("CARD");
        request.setCurrency("INR");

        when(paymentRepository.findByOrderId(700L)).thenReturn(Optional.empty());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("id", "order_abc123"), HttpStatus.OK));

        RazorpayOrderResponse response = paymentService.createRazorpayOrder(request);

        assertEquals("order_abc123", response.getRazorpayOrderId());
        assertEquals(700L, response.getOrderId());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createRazorpayCheckoutOrder_shouldFallbackWhenGatewayFails() {
        RazorpayCheckoutRequest request = new RazorpayCheckoutRequest();
        request.setAmount(499.0);
        request.setMode("UPI");
        request.setCurrency("INR");

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("gateway down"));

        RazorpayOrderResponse response = paymentService.createRazorpayCheckoutOrder(request);

        assertThat(response.getRazorpayOrderId()).startsWith("QB-MOCK-");
        assertEquals("PENDING", response.getStatus());
    }

    @Test
    void payFromWallet_shouldCreateStatementAndDebitWallet() {
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setOrderId(800L);
        request.setCustomerId(35L);
        request.setAmount(250.0);
        request.setMode("WALLET");
        request.setCurrency("INR");

        when(walletRepository.findByCustomerId(35L)).thenReturn(Optional.of(wallet));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.payFromWallet(request);

        assertEquals("PAID", response.getStatus());
        assertEquals("WALLET", response.getMode());
        verify(walletRepository).debitBalance(35L, 250.0);
        verify(statementRepository).save(any());
    }

    @Test
    void payFromWallet_shouldRejectLowBalance() {
        wallet.setBalance(100.0);

        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setOrderId(801L);
        request.setCustomerId(35L);
        request.setAmount(250.0);
        request.setMode("WALLET");
        request.setCurrency("INR");

        when(walletRepository.findByCustomerId(35L)).thenReturn(Optional.of(wallet));

        assertThrows(InsufficientBalanceException.class, () -> paymentService.payFromWallet(request));
        verify(walletRepository, never()).debitBalance(anyLong(), anyDouble());
    }

    @Test
    void refundPayment_shouldCreditWalletAndMarkRefunded() {
        RefundRequest request = new RefundRequest();
        request.setPaymentId(101L);
        request.setRefundTo("WALLET");
        request.setReason("Order cancelled");

        when(paymentRepository.findById(101L)).thenReturn(Optional.of(paidPayment));
        when(walletRepository.findByCustomerId(35L)).thenReturn(Optional.of(wallet));
        when(statementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.refundPayment(101L, request);

        assertEquals("REFUNDED", response.getStatus());
        assertThat(response.getRefundTransactionId()).isNotBlank();
        verify(walletRepository).creditBalance(35L, 250.0);
        verify(paymentRepository).updateToRefunded(eq(101L), eq("REFUNDED"), any(), anyString());
    }

    @Test
    void refundPayment_shouldRejectAlreadyRefundedPayment() {
        RefundRequest request = new RefundRequest();
        request.setPaymentId(102L);

        when(paymentRepository.findById(102L)).thenReturn(Optional.of(refundedPayment));

        assertThrows(PaymentAlreadyRefundedException.class,
                () -> paymentService.refundPayment(102L, request));
    }

    @Test
    void getOrCreateWallet_shouldCreateWalletWhenMissing() {
        when(walletRepository.findByCustomerId(44L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse response = paymentService.getOrCreateWallet(44L);

        assertEquals(44L, response.getCustomerId());
        assertEquals(0.0, response.getBalance(), 0.01);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void getByCustomerId_shouldWrapPagedResults() {
        when(paymentRepository.findByCustomerIdOrderByCreatedAtDesc(eq(35L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(paidPayment)));

        var response = paymentService.getByCustomerId(35L, PageRequest.of(0, 10));

        assertEquals(1, response.getContent().size());
        assertEquals(500L, response.getContent().get(0).getOrderId());
    }
}
