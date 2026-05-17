package com.atlascommerce.pricing_service.controller;

import com.atlascommerce.pricing_service.dto.PricingRequest;
import com.atlascommerce.pricing_service.dto.PricingResponse;
import com.atlascommerce.pricing_service.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    public PricingResponse calculate(@Valid @RequestBody PricingRequest request) {
        return pricingService.calculate(request);
    }
}