package com.atlascommerce.pricing_service.service;

import com.atlascommerce.pricing_service.dto.PricingItemResponse;
import com.atlascommerce.pricing_service.dto.PricingRequest;
import com.atlascommerce.pricing_service.dto.PricingResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PricingService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public PricingResponse calculate(PricingRequest request) {
        List<PricingItemResponse> items = request.items()
                .stream()
                .map(item -> {
                    BigDecimal subtotal = item.unitPrice()
                            .multiply(BigDecimal.valueOf(item.quantity()))
                            .setScale(2, RoundingMode.HALF_UP);

                    return new PricingItemResponse(
                            item.productId(),
                            item.quantity(),
                            item.unitPrice().setScale(2, RoundingMode.HALF_UP),
                            subtotal
                    );
                })
                .toList();

        BigDecimal subtotal = items.stream()
                .map(PricingItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountAmount = subtotal
                .multiply(request.discountPercentage())
                .divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxableAmount = subtotal
                .subtract(discountAmount)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxAmount = taxableAmount
                .multiply(request.taxPercentage())
                .divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = taxableAmount
                .add(taxAmount)
                .setScale(2, RoundingMode.HALF_UP);

        return new PricingResponse(
                items,
                subtotal,
                discountAmount,
                taxableAmount,
                taxAmount,
                total,
                request.currency().toUpperCase()
        );
    }
}