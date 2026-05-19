package com.atlascommerce.shipping_service.mapper;

import com.atlascommerce.shipping_service.dto.ShipmentResponse;
import com.atlascommerce.shipping_service.entity.Shipment;
import org.springframework.stereotype.Component;

@Component
public class ShipmentMapper {

    public ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getOrderId(),
                shipment.getUserId(),
                shipment.getStatus(),
                shipment.getProvider(),
                shipment.getTrackingNumber(),
                shipment.getRecipientName(),
                shipment.getAddressLine(),
                shipment.getCity(),
                shipment.getCountry(),
                shipment.getPostalCode(),
                shipment.getFailureReason(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt()
        );
    }
}