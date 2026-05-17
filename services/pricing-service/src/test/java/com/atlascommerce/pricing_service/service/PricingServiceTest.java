package com.atlascommerce.pricing_service.service;

import com.atlascommerce.pricing_service.dto.PricingItemRequest;
import com.atlascommerce.pricing_service.dto.PricingRequest;
import com.atlascommerce.pricing_service.dto.PricingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class PricingServiceTest {

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService();
    }

    @Test
    void calculate_shouldReturnCorrectPricing() {

        PricingRequest request = new PricingRequest(
                List.of(
                        new PricingItemRequest(
                                101L,
                                2,
                                new BigDecimal("19.99")
                        ),
                        new PricingItemRequest(
                                202L,
                                1,
                                new BigDecimal("49.99")
                        )
                ),
                new BigDecimal("10"),
                new BigDecimal("21"),
                "EUR"
        );

        PricingResponse response = pricingService.calculate(request);

        assertThat(response).isNotNull();

        assertThat(response.items()).hasSize(2);

        assertThat(response.subtotal())
                .isEqualByComparingTo("89.97");

        assertThat(response.discountAmount())
                .isEqualByComparingTo("9.00");

        assertThat(response.taxableAmount())
                .isEqualByComparingTo("80.97");

        assertThat(response.taxAmount())
                .isEqualByComparingTo("17.00");

        assertThat(response.total())
                .isEqualByComparingTo("97.97");

        assertThat(response.currency())
                .isEqualTo("EUR");
    }

    @Test
    void calculate_shouldHandleZeroDiscount() {

        PricingRequest request = new PricingRequest(
                List.of(
                        new PricingItemRequest(
                                101L,
                                1,
                                new BigDecimal("100.00")
                        )
                ),
                BigDecimal.ZERO,
                new BigDecimal("21"),
                "EUR"
        );

        PricingResponse response = pricingService.calculate(request);

        assertThat(response.subtotal())
                .isEqualByComparingTo("100.00");

        assertThat(response.discountAmount())
                .isEqualByComparingTo("0.00");

        assertThat(response.taxableAmount())
                .isEqualByComparingTo("100.00");

        assertThat(response.taxAmount())
                .isEqualByComparingTo("21.00");

        assertThat(response.total())
                .isEqualByComparingTo("121.00");
    }

    @Test
    void calculate_shouldHandleZeroTax() {

        PricingRequest request = new PricingRequest(
                List.of(
                        new PricingItemRequest(
                                101L,
                                1,
                                new BigDecimal("50.00")
                        )
                ),
                new BigDecimal("20"),
                BigDecimal.ZERO,
                "USD"
        );

        PricingResponse response = pricingService.calculate(request);

        assertThat(response.subtotal())
                .isEqualByComparingTo("50.00");

        assertThat(response.discountAmount())
                .isEqualByComparingTo("10.00");

        assertThat(response.taxableAmount())
                .isEqualByComparingTo("40.00");

        assertThat(response.taxAmount())
                .isEqualByComparingTo("0.00");

        assertThat(response.total())
                .isEqualByComparingTo("40.00");

        assertThat(response.currency())
                .isEqualTo("USD");
    }

    @Test
    void calculate_shouldRoundCorrectly() {

        PricingRequest request = new PricingRequest(
                List.of(
                        new PricingItemRequest(
                                101L,
                                3,
                                new BigDecimal("19.995")
                        )
                ),
                new BigDecimal("12.5"),
                new BigDecimal("21"),
                "EUR"
        );

        PricingResponse response = pricingService.calculate(request);

        assertThat(response.subtotal())
                .isEqualByComparingTo("59.99");

        assertThat(response.discountAmount())
                .isEqualByComparingTo("7.50");

        assertThat(response.taxableAmount())
                .isEqualByComparingTo("52.49");

        assertThat(response.taxAmount())
                .isEqualByComparingTo("11.02");

        assertThat(response.total())
                .isEqualByComparingTo("63.51");
    }
}