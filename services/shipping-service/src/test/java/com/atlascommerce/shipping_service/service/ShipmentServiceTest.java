package com.atlascommerce.shipping_service.service;

import com.atlascommerce.shipping_service.dto.*;
import com.atlascommerce.shipping_service.entity.Shipment;
import com.atlascommerce.shipping_service.enums.ShipmentStatus;
import com.atlascommerce.shipping_service.enums.ShippingProvider;
import com.atlascommerce.shipping_service.exception.InvalidShipmentStateException;
import com.atlascommerce.shipping_service.exception.ShipmentAlreadyExistsException;
import com.atlascommerce.shipping_service.mapper.ShipmentMapper;
import com.atlascommerce.shipping_service.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ShipmentServiceTest {

    @Mock ShipmentRepository repository;
    @Mock ShipmentMapper mapper;

    @InjectMocks ShipmentService shipmentService;

    private Shipment shipment;
    private ShipmentResponse response;

    @BeforeEach
    void setUp() {

        shipment = Shipment.builder()
                .id(1L)
                .orderId(1001L)
                .userId(1L)
                .status(ShipmentStatus.CREATED)
                .provider(ShippingProvider.MOCK)
                .trackingNumber("TRK-123")
                .recipientName("Jose")
                .addressLine("Street 123")
                .city("Barcelona")
                .country("Spain")
                .postalCode("08001")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        response = new ShipmentResponse(
                1L,
                1001L,
                1L,
                ShipmentStatus.CREATED,
                ShippingProvider.MOCK,
                "TRK-123",
                "Jose",
                "Street 123",
                "Barcelona",
                "Spain",
                "08001",
                null,
                shipment.getCreatedAt(),
                shipment.getUpdatedAt()
        );
    }

    @Test
    void create_shouldCreateShipmentSuccessfully() {

        var request = new CreateShipmentRequest(
                1001L,
                1L,
                ShippingProvider.MOCK,
                "Jose",
                "Street 123",
                "Barcelona",
                "Spain",
                "08001"
        );

        when(repository.existsByOrderId(1001L)).thenReturn(false);
        when(repository.save(any(Shipment.class))).thenReturn(shipment);
        when(mapper.toResponse(any(Shipment.class))).thenReturn(response);

        ShipmentResponse result = shipmentService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(ShipmentStatus.CREATED);

        verify(repository).save(any(Shipment.class));
    }

    @Test
    void create_shouldThrowException_whenShipmentAlreadyExists() {

        var request = new CreateShipmentRequest(
                1001L,
                1L,
                ShippingProvider.MOCK,
                "Jose",
                "Street 123",
                "Barcelona",
                "Spain",
                "08001"
        );

        when(repository.existsByOrderId(1001L)).thenReturn(true);

        assertThatThrownBy(() -> shipmentService.create(request))
                .isInstanceOf(ShipmentAlreadyExistsException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void markInTransit_shouldMoveShipmentToInTransit() {

        when(repository.findById(1L)).thenReturn(Optional.of(shipment));
        when(repository.save(any(Shipment.class))).thenReturn(shipment);
        when(mapper.toResponse(any(Shipment.class))).thenReturn(response);

        shipmentService.markInTransit(1L);

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
    }

    @Test
    void markDelivered_shouldMoveShipmentToDelivered() {

        shipment.setStatus(ShipmentStatus.IN_TRANSIT);

        when(repository.findById(1L)).thenReturn(Optional.of(shipment));
        when(repository.save(any(Shipment.class))).thenReturn(shipment);
        when(mapper.toResponse(any(Shipment.class))).thenReturn(response);

        shipmentService.markDelivered(1L);

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
    }

    @Test
    void markDelivered_shouldThrow_whenShipmentNotInTransit() {

        shipment.setStatus(ShipmentStatus.CREATED);

        when(repository.findById(1L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.markDelivered(1L))
                .isInstanceOf(InvalidShipmentStateException.class);
    }

    @Test
    void fail_shouldFailShipment() {

        var request = new FailShipmentRequest("Carrier issue");

        when(repository.findById(1L)).thenReturn(Optional.of(shipment));
        when(repository.save(any(Shipment.class))).thenReturn(shipment);
        when(mapper.toResponse(any(Shipment.class))).thenReturn(response);

        shipmentService.fail(1L, request);

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.FAILED);
        assertThat(shipment.getFailureReason()).isEqualTo("Carrier issue");
    }

    @Test
    void cancel_shouldCancelShipment() {

        when(repository.findById(1L)).thenReturn(Optional.of(shipment));
        when(repository.save(any(Shipment.class))).thenReturn(shipment);
        when(mapper.toResponse(any(Shipment.class))).thenReturn(response);

        shipmentService.cancel(1L);

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
    }

    @Test
    void cancel_shouldThrow_whenShipmentDelivered() {

        shipment.setStatus(ShipmentStatus.DELIVERED);

        when(repository.findById(1L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.cancel(1L))
                .isInstanceOf(InvalidShipmentStateException.class);
    }
}