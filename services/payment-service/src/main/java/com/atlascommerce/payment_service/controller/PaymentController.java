package com.atlascommerce.payment_service.controller;

import com.atlascommerce.payment_service.dto.*;
import com.atlascommerce.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    public PaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    public PaymentResponse getById(@PathVariable Long id) {
        return paymentService.getById(id);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    public PaymentResponse getByOrderId(@PathVariable Long orderId) {
        return paymentService.getByOrderId(orderId);
    }

    @PostMapping("/{id}/capture")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public PaymentResponse capture(
            @PathVariable Long id,
            @Valid @RequestBody CapturePaymentRequest request
    ) {
        return paymentService.capture(id, request);
    }

    @PostMapping("/{id}/fail")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public PaymentResponse fail(
            @PathVariable Long id,
            @Valid @RequestBody FailPaymentRequest request
    ) {
        return paymentService.fail(id, request);
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public PaymentResponse refund(@PathVariable Long id) {
        return paymentService.refund(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    public PaymentResponse cancel(@PathVariable Long id) {
        return paymentService.cancel(id);
    }
}