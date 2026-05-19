package com.atlascommerce.payment_service.service;

import com.atlascommerce.payment_service.dto.*;
import com.atlascommerce.payment_service.entity.Payment;
import com.atlascommerce.payment_service.enums.PaymentStatus;
import com.atlascommerce.payment_service.exception.InvalidPaymentStateException;
import com.atlascommerce.payment_service.exception.PaymentNotFoundException;
import com.atlascommerce.payment_service.mapper.PaymentMapper;
import com.atlascommerce.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentMapper mapper;

    @Transactional
    public PaymentResponse create(CreatePaymentRequest request) {
        return repository.findByIdempotencyKey(request.idempotencyKey())
                .map(mapper::toResponse)
                .orElseGet(() -> {
                    Payment payment = Payment.builder()
                            .orderId(request.orderId())
                            .userId(request.userId())
                            .amount(request.amount())
                            .currency(request.currency().toUpperCase())
                            .provider(request.provider())
                            .status(PaymentStatus.AUTHORIZED)
                            .idempotencyKey(request.idempotencyKey())
                            .providerTransactionId("MOCK-" + UUID.randomUUID())
                            .build();

                    return mapper.toResponse(repository.save(payment));
                });
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        return mapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(Long orderId) {
        Payment payment = repository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));

        return mapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse capture(Long id, CapturePaymentRequest request) {
        Payment payment = findById(id);

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new InvalidPaymentStateException("Only AUTHORIZED payments can be captured");
        }

        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setProviderTransactionId(request.providerTransactionId());

        return mapper.toResponse(repository.save(payment));
    }

    @Transactional
    public PaymentResponse fail(Long id, FailPaymentRequest request) {
        Payment payment = findById(id);

        if (payment.getStatus() == PaymentStatus.CAPTURED || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new InvalidPaymentStateException("Captured or refunded payments cannot be marked as failed");
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(request.failureReason());

        return mapper.toResponse(repository.save(payment));
    }

    @Transactional
    public PaymentResponse refund(Long id) {
        Payment payment = findById(id);

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new InvalidPaymentStateException("Only CAPTURED payments can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);

        return mapper.toResponse(repository.save(payment));
    }

    @Transactional
    public PaymentResponse cancel(Long id) {
        Payment payment = findById(id);

        if (payment.getStatus() != PaymentStatus.AUTHORIZED && payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException("Only PENDING or AUTHORIZED payments can be cancelled");
        }

        payment.setStatus(PaymentStatus.CANCELLED);

        return mapper.toResponse(repository.save(payment));
    }

    private Payment findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }
}