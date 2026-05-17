package com.atlascommerce.coupon_service.service;

import com.atlascommerce.coupon_service.dto.*;
import com.atlascommerce.coupon_service.entity.Coupon;
import com.atlascommerce.coupon_service.enums.DiscountType;
import com.atlascommerce.coupon_service.exception.CouponNotFoundException;
import com.atlascommerce.coupon_service.exception.DuplicateCouponException;
import com.atlascommerce.coupon_service.exception.InvalidCouponException;
import com.atlascommerce.coupon_service.mapper.CouponMapper;
import com.atlascommerce.coupon_service.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CouponService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final CouponRepository repository;
    private final CouponMapper mapper;

    @Transactional
    public CouponResponse create(CreateCouponRequest request) {
        String normalizedCode = normalizeCode(request.code());

        if (repository.existsByCode(normalizedCode)) {
            throw new DuplicateCouponException(normalizedCode);
        }

        if (request.validUntil().isBefore(request.validFrom())) {
            throw new InvalidCouponException("validUntil must be after validFrom");
        }

        Coupon coupon = Coupon.builder()
                .code(normalizedCode)
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .minimumOrderAmount(request.minimumOrderAmount())
                .maxUses(request.maxUses())
                .currentUses(0)
                .active(true)
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .build();

        return mapper.toResponse(repository.save(coupon));
    }

    @Transactional(readOnly = true)
    public CouponResponse getByCode(String code) {
        return mapper.toResponse(findByCode(code));
    }

    @Transactional(readOnly = true)
    public CouponValidationResponse validate(CouponValidationRequest request) {
        Coupon coupon = repository.findByCode(normalizeCode(request.code()))
                .orElse(null);

        if (coupon == null) {
            return new CouponValidationResponse(request.code(), false, "Coupon not found");
        }

        String invalidReason = getInvalidReason(coupon, request.orderAmount());

        if (invalidReason != null) {
            return new CouponValidationResponse(coupon.getCode(), false, invalidReason);
        }

        return new CouponValidationResponse(coupon.getCode(), true, "Coupon is valid");
    }

    @Transactional
    public CouponApplyResponse apply(CouponApplyRequest request) {
        Coupon coupon = findByCode(request.code());

        String invalidReason = getInvalidReason(coupon, request.orderAmount());
        if (invalidReason != null) {
            throw new InvalidCouponException(invalidReason);
        }

        BigDecimal discountAmount = calculateDiscount(coupon, request.orderAmount());
        BigDecimal finalAmount = request.orderAmount()
                .subtract(discountAmount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        coupon.setCurrentUses(coupon.getCurrentUses() + 1);
        repository.save(coupon);

        return new CouponApplyResponse(
                coupon.getCode(),
                request.orderAmount().setScale(2, RoundingMode.HALF_UP),
                discountAmount,
                finalAmount
        );
    }

    @Transactional
    public CouponResponse deactivate(String code) {
        Coupon coupon = findByCode(code);
        coupon.setActive(false);
        return mapper.toResponse(repository.save(coupon));
    }

    private Coupon findByCode(String code) {
        return repository.findByCode(normalizeCode(code))
                .orElseThrow(() -> new CouponNotFoundException(normalizeCode(code)));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private String getInvalidReason(Coupon coupon, BigDecimal orderAmount) {
        Instant now = Instant.now();

        if (!coupon.getActive()) {
            return "Coupon is inactive";
        }

        if (now.isBefore(coupon.getValidFrom())) {
            return "Coupon is not active yet";
        }

        if (now.isAfter(coupon.getValidUntil())) {
            return "Coupon has expired";
        }

        if (coupon.getCurrentUses() >= coupon.getMaxUses()) {
            return "Coupon usage limit reached";
        }

        if (orderAmount.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            return "Order amount is below minimum required";
        }

        return null;
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        BigDecimal discount;

        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = orderAmount
                    .multiply(coupon.getDiscountValue())
                    .divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP);
        } else {
            discount = coupon.getDiscountValue();
        }

        return discount
                .min(orderAmount)
                .setScale(2, RoundingMode.HALF_UP);
    }
}