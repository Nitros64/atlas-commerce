package com.atlascommerce.shipping_service.service;

import com.atlascommerce.shipping_service.dto.*;
import com.atlascommerce.shipping_service.entity.Shipment;
import com.atlascommerce.shipping_service.enums.ShipmentStatus;
import com.atlascommerce.shipping_service.exception.InvalidShipmentStateException;
import com.atlascommerce.shipping_service.exception.ShipmentAlreadyExistsException;
import com.atlascommerce.shipping_service.exception.ShipmentNotFoundException;
import com.atlascommerce.shipping_service.mapper.ShipmentMapper;
import com.atlascommerce.shipping_service.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository repository;
    private final ShipmentMapper mapper;

    @Transactional
    public ShipmentResponse create(CreateShipmentRequest request) {

        if (repository.existsByOrderId(request.orderId())) {
            throw new ShipmentAlreadyExistsException(request.orderId());
        }

        Shipment shipment = Shipment.builder()
                .orderId(request.orderId())
                .userId(request.userId())
                .provider(request.provider())
                .trackingNumber("TRK-" + UUID.randomUUID())
                .recipientName(request.recipientName())
                .addressLine(request.addressLine())
                .city(request.city())
                .country(request.country())
                .postalCode(request.postalCode())
                .status(ShipmentStatus.CREATED)
                .build();

        return mapper.toResponse(repository.save(shipment));
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getById(Long id) {
        return mapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getByOrderId(Long orderId) {
        Shipment shipment = repository.findByOrderId(orderId)
                .orElseThrow(() -> new ShipmentNotFoundException(orderId));

        return mapper.toResponse(shipment);
    }

    @Transactional
    public ShipmentResponse markInTransit(Long id) {
        Shipment shipment = findById(id);

        if (shipment.getStatus() != ShipmentStatus.CREATED) {
            throw new InvalidShipmentStateException(
                    "Only CREATED shipments can move to IN_TRANSIT"
            );
        }

        shipment.setStatus(ShipmentStatus.IN_TRANSIT);

        return mapper.toResponse(repository.save(shipment));
    }

    @Transactional
    public ShipmentResponse markDelivered(Long id) {
        Shipment shipment = findById(id);

        if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
            throw new InvalidShipmentStateException(
                    "Only IN_TRANSIT shipments can be DELIVERED"
            );
        }

        shipment.setStatus(ShipmentStatus.DELIVERED);

        return mapper.toResponse(repository.save(shipment));
    }

    @Transactional
    public ShipmentResponse fail(Long id, FailShipmentRequest request) {
        Shipment shipment = findById(id);

        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new InvalidShipmentStateException(
                    "Delivered shipments cannot fail"
            );
        }

        shipment.setStatus(ShipmentStatus.FAILED);
        shipment.setFailureReason(request.failureReason());

        return mapper.toResponse(repository.save(shipment));
    }

    @Transactional
    public ShipmentResponse cancel(Long id) {
        Shipment shipment = findById(id);

        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new InvalidShipmentStateException(
                    "Delivered shipments cannot be cancelled"
            );
        }

        shipment.setStatus(ShipmentStatus.CANCELLED);

        return mapper.toResponse(repository.save(shipment));
    }

    private Shipment findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ShipmentNotFoundException(id));
    }
}