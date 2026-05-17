package com.atlascommerce.coupon_service.controller;

import com.atlascommerce.coupon_service.dto.*;
import com.atlascommerce.coupon_service.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public CouponResponse create(@Valid @RequestBody CreateCouponRequest request) {
        return couponService.create(request);
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public CouponResponse getByCode(@PathVariable String code) {
        return couponService.getByCode(code);
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    public CouponValidationResponse validate(@Valid @RequestBody CouponValidationRequest request) {
        return couponService.validate(request);
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SYSTEM')")
    public CouponApplyResponse apply(@Valid @RequestBody CouponApplyRequest request) {
        return couponService.apply(request);
    }

    @PatchMapping("/{code}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public CouponResponse deactivate(@PathVariable String code) {
        return couponService.deactivate(code);
    }
}