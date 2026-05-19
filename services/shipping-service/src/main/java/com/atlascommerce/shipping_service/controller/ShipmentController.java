package com.atlascommerce.shipping_service.controller;

import com.atlascommerce.shipping_service.dto.*;
import com.atlascommerce.shipping_service.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shipping")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    public ShipmentResponse create(@Valid @RequestBody CreateShipmentRequest request) {
        return shipmentService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    public ShipmentResponse getById(@PathVariable Long id) {
        return shipmentService.getById(id);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    public ShipmentResponse getByOrderId(@PathVariable Long orderId) {
        return shipmentService.getByOrderId(orderId);
    }

    @PostMapping("/{id}/in-transit")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public ShipmentResponse markInTransit(@PathVariable Long id) {
        return shipmentService.markInTransit(id);
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public ShipmentResponse markDelivered(@PathVariable Long id) {
        return shipmentService.markDelivered(id);
    }

    @PostMapping("/{id}/fail")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public ShipmentResponse fail(
            @PathVariable Long id,
            @Valid @RequestBody FailShipmentRequest request
    ) {
        return shipmentService.fail(id, request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    public ShipmentResponse cancel(@PathVariable Long id) {
        return shipmentService.cancel(id);
    }
}