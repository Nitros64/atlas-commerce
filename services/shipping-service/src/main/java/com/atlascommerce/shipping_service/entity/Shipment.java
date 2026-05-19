package com.atlascommerce.shipping_service.entity;

import com.atlascommerce.shipping_service.enums.ShipmentStatus;
import com.atlascommerce.shipping_service.enums.ShippingProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "shipments",
        indexes = {
                @Index(name = "idx_shipment_order_id", columnList = "orderId"),
                @Index(name = "idx_shipment_tracking_number", columnList = "trackingNumber")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ShipmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ShippingProvider provider;

    @Column(nullable = false, unique = true, length = 120)
    private String trackingNumber;

    @Column(nullable = false, length = 150)
    private String recipientName;

    @Column(nullable = false, length = 255)
    private String addressLine;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 30)
    private String postalCode;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (status == null) {
            status = ShipmentStatus.CREATED;
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}