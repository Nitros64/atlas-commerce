package com.atlascommerce.payment_service.service;

import com.atlascommerce.payment_service.dto.*;
import com.atlascommerce.payment_service.entity.Payment;
import com.atlascommerce.payment_service.enums.PaymentProvider;
import com.atlascommerce.payment_service.enums.PaymentStatus;
import com.atlascommerce.payment_service.exception.InvalidPaymentStateException;
import com.atlascommerce.payment_service.mapper.PaymentMapper;
import com.atlascommerce.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository repository;
    @Mock PaymentMapper mapper;
    @InjectMocks PaymentService paymentService;

    private Payment payment;
    private PaymentResponse response;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .id(1L)
                .orderId(100L)
                .userId(1L)
                .amount(new BigDecimal("50.00"))
                .currency("EUR")
                .status(PaymentStatus.AUTHORIZED)
                .provider(PaymentProvider.MOCK)
                .idempotencyKey("idem-123")
                .providerTransactionId("MOCK-123")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        response = new PaymentResponse(
                1L, 100L, 1L, new BigDecimal("50.00"), "EUR",
                PaymentStatus.AUTHORIZED, PaymentProvider.MOCK,
                "idem-123", "MOCK-123", null,
                payment.getCreatedAt(), payment.getUpdatedAt()
        );
    }

    @Test
    void create_shouldCreatePayment_whenIdempotencyKeyDoesNotExist() {
        var request = new CreatePaymentRequest(
                100L, 1L, new BigDecimal("50.00"), "eur",
                PaymentProvider.MOCK, "idem-123"
        );

        when(repository.findByIdempotencyKey("idem-123")).thenReturn(Optional.empty());
        when(repository.save(any(Payment.class))).thenReturn(payment);
        when(mapper.toResponse(any(Payment.class))).thenReturn(response);

        PaymentResponse result = paymentService.create(request);

        assertThat(result.status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(result.currency()).isEqualTo("EUR");

        verify(repository).save(argThat(saved ->
                saved.getOrderId().equals(100L) &&
                saved.getUserId().equals(1L) &&
                saved.getCurrency().equals("EUR") &&
                saved.getStatus() == PaymentStatus.AUTHORIZED
        ));
    }

    @Test
    void create_shouldReturnExistingPayment_whenIdempotencyKeyExists() {
        var request = new CreatePaymentRequest(
                100L, 1L, new BigDecimal("50.00"), "EUR",
                PaymentProvider.MOCK, "idem-123"
        );

        when(repository.findByIdempotencyKey("idem-123")).thenReturn(Optional.of(payment));
        when(mapper.toResponse(payment)).thenReturn(response);

        PaymentResponse result = paymentService.create(request);

        assertThat(result.id()).isEqualTo(1L);
        verify(repository, never()).save(any());
    }

    @Test
    void capture_shouldCaptureAuthorizedPayment() {
        var request = new CapturePaymentRequest("TX-999");

        when(repository.findById(1L)).thenReturn(Optional.of(payment));
        when(repository.save(any(Payment.class))).thenReturn(payment);
        when(mapper.toResponse(any(Payment.class))).thenReturn(response);

        paymentService.capture(1L, request);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(payment.getProviderTransactionId()).isEqualTo("TX-999");
    }

    @Test
    void capture_shouldThrow_whenPaymentIsNotAuthorized() {
        payment.setStatus(PaymentStatus.FAILED);

        when(repository.findById(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.capture(1L, new CapturePaymentRequest("TX-999")))
                .isInstanceOf(InvalidPaymentStateException.class);
    }

    @Test
    void refund_shouldRefundCapturedPayment() {
        payment.setStatus(PaymentStatus.CAPTURED);

        when(repository.findById(1L)).thenReturn(Optional.of(payment));
        when(repository.save(any(Payment.class))).thenReturn(payment);
        when(mapper.toResponse(any(Payment.class))).thenReturn(response);

        paymentService.refund(1L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void refund_shouldThrow_whenPaymentIsNotCaptured() {
        payment.setStatus(PaymentStatus.AUTHORIZED);

        when(repository.findById(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refund(1L))
                .isInstanceOf(InvalidPaymentStateException.class);
    }

    @Test
    void cancel_shouldCancelAuthorizedPayment() {
        when(repository.findById(1L)).thenReturn(Optional.of(payment));
        when(repository.save(any(Payment.class))).thenReturn(payment);
        when(mapper.toResponse(any(Payment.class))).thenReturn(response);

        paymentService.cancel(1L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void fail_shouldMarkPaymentAsFailed() {
        var request = new FailPaymentRequest("Insufficient funds");

        when(repository.findById(1L)).thenReturn(Optional.of(payment));
        when(repository.save(any(Payment.class))).thenReturn(payment);
        when(mapper.toResponse(any(Payment.class))).thenReturn(response);

        paymentService.fail(1L, request);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("Insufficient funds");
    }
}