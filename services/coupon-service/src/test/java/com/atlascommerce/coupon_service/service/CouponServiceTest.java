package com.atlascommerce.coupon_service.service;

import com.atlascommerce.coupon_service.dto.*;
import com.atlascommerce.coupon_service.entity.Coupon;
import com.atlascommerce.coupon_service.enums.DiscountType;
import com.atlascommerce.coupon_service.exception.DuplicateCouponException;
import com.atlascommerce.coupon_service.exception.InvalidCouponException;
import com.atlascommerce.coupon_service.mapper.CouponMapper;
import com.atlascommerce.coupon_service.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository repository;

    @Mock
    private CouponMapper mapper;

    @InjectMocks
    private CouponService couponService;

    private Coupon coupon;
    private CouponResponse response;

    @BeforeEach
    void setUp() {
        coupon = Coupon.builder()
                .id(1L)
                .code("WELCOME10")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .minimumOrderAmount(new BigDecimal("20.00"))
                .maxUses(100)
                .currentUses(0)
                .active(true)
                .validFrom(Instant.now().minusSeconds(3600))
                .validUntil(Instant.now().plusSeconds(86400))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        response = new CouponResponse(
                1L,
                "WELCOME10",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                100,
                0,
                true,
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                coupon.getCreatedAt(),
                coupon.getUpdatedAt()
        );
    }

    @Test
    void create_shouldCreateCouponSuccessfully() {
        CreateCouponRequest request = new CreateCouponRequest(
                "welcome10",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                100,
                Instant.now().minusSeconds(3600),
                Instant.now().plusSeconds(86400)
        );

        when(repository.existsByCode("WELCOME10")).thenReturn(false);
        when(repository.save(any(Coupon.class))).thenReturn(coupon);
        when(mapper.toResponse(any(Coupon.class))).thenReturn(response);

        CouponResponse result = couponService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.code()).isEqualTo("WELCOME10");

        verify(repository).save(argThat(saved ->
                saved.getCode().equals("WELCOME10") &&
                saved.getDiscountType() == DiscountType.PERCENTAGE &&
                saved.getActive()
        ));
    }

    @Test
    void create_shouldThrowException_whenCouponAlreadyExists() {
        CreateCouponRequest request = new CreateCouponRequest(
                "WELCOME10",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                100,
                Instant.now().minusSeconds(3600),
                Instant.now().plusSeconds(86400)
        );

        when(repository.existsByCode("WELCOME10")).thenReturn(true);

        assertThatThrownBy(() -> couponService.create(request))
                .isInstanceOf(DuplicateCouponException.class)
                .hasMessageContaining("WELCOME10");

        verify(repository, never()).save(any());
    }

    @Test
    void create_shouldThrowException_whenValidUntilIsBeforeValidFrom() {
        Instant validFrom = Instant.now().plusSeconds(86400);
        Instant validUntil = Instant.now();

        CreateCouponRequest request = new CreateCouponRequest(
                "WELCOME10",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                100,
                validFrom,
                validUntil
        );

        when(repository.existsByCode("WELCOME10")).thenReturn(false);

        assertThatThrownBy(() -> couponService.create(request))
                .isInstanceOf(InvalidCouponException.class)
                .hasMessageContaining("validUntil");

        verify(repository, never()).save(any());
    }

    @Test
    void validate_shouldReturnValid_whenCouponIsValid() {
        CouponValidationRequest request =
                new CouponValidationRequest("WELCOME10", new BigDecimal("50.00"));

        when(repository.findByCode("WELCOME10")).thenReturn(Optional.of(coupon));

        CouponValidationResponse result = couponService.validate(request);

        assertThat(result.valid()).isTrue();
        assertThat(result.reason()).isEqualTo("Coupon is valid");
    }

    @Test
    void validate_shouldReturnInvalid_whenCouponNotFound() {
        CouponValidationRequest request =
                new CouponValidationRequest("UNKNOWN", new BigDecimal("50.00"));

        when(repository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        CouponValidationResponse result = couponService.validate(request);

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).isEqualTo("Coupon not found");
    }

    @Test
    void validate_shouldReturnInvalid_whenCouponInactive() {
        coupon.setActive(false);

        CouponValidationRequest request =
                new CouponValidationRequest("WELCOME10", new BigDecimal("50.00"));

        when(repository.findByCode("WELCOME10")).thenReturn(Optional.of(coupon));

        CouponValidationResponse result = couponService.validate(request);

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).isEqualTo("Coupon is inactive");
    }

    @Test
    void validate_shouldReturnInvalid_whenCouponExpired() {
        coupon.setValidUntil(Instant.now().minusSeconds(60));

        CouponValidationRequest request =
                new CouponValidationRequest("WELCOME10", new BigDecimal("50.00"));

        when(repository.findByCode("WELCOME10")).thenReturn(Optional.of(coupon));

        CouponValidationResponse result = couponService.validate(request);

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).isEqualTo("Coupon has expired");
    }

    @Test
    void validate_shouldReturnInvalid_whenUsageLimitReached() {
        coupon.setCurrentUses(100);

        CouponValidationRequest request =
                new CouponValidationRequest("WELCOME10", new BigDecimal("50.00"));

        when(repository.findByCode("WELCOME10")).thenReturn(Optional.of(coupon));

        CouponValidationResponse result = couponService.validate(request);

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).isEqualTo("Coupon usage limit reached");
    }

    @Test
    void validate_shouldReturnInvalid_whenOrderAmountIsBelowMinimum() {
        CouponValidationRequest request =
                new CouponValidationRequest("WELCOME10", new BigDecimal("10.00"));

        when(repository.findByCode("WELCOME10")).thenReturn(Optional.of(coupon));

        CouponValidationResponse result = couponService.validate(request);

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).isEqualTo("Order amount is below minimum required");
    }

    @Test
    void apply_shouldApplyPercentageCouponSuccessfully() {
        CouponApplyRequest request =
                new CouponApplyRequest("WELCOME10", new BigDecimal("50.00"));

        when(repository.findByCode("WELCOME10")).thenReturn(Optional.of(coupon));
        when(repository.save(any(Coupon.class))).thenReturn(coupon);

        CouponApplyResponse result = couponService.apply(request);

        assertThat(result.code()).isEqualTo("WELCOME10");
        assertThat(result.originalAmount()).isEqualByComparingTo("50.00");
        assertThat(result.discountAmount()).isEqualByComparingTo("5.00");
        assertThat(result.finalAmount()).isEqualByComparingTo("45.00");
        assertThat(coupon.getCurrentUses()).isEqualTo(1);

        verify(repository).save(coupon);
    }

    @Test
    void apply_shouldApplyFixedAmountCouponSuccessfully() {
        coupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        coupon.setDiscountValue(new BigDecimal("15.00"));

        CouponApplyRequest request =
                new CouponApplyRequest("WELCOME10", new BigDecimal("50.00"));

        when(repository.findByCode("WELCOME10")).thenReturn(Optional.of(coupon));
        when(repository.save(any(Coupon.class))).thenReturn(coupon);

        CouponApplyResponse result = couponService.apply(request);

        assertThat(result.discountAmount()).isEqualByComparingTo("15.00");
        assertThat(result.finalAmount()).isEqualByComparingTo("35.00");
    }

    @Test
    void apply_shouldNotMakeFinalAmountNegative() {
        coupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        coupon.setDiscountValue(new BigDecimal("100.00"));

        CouponApplyRequest request =
                new CouponApplyRequest("WELCOME10", new BigDecimal("50.00"));

        when(repository.findByCode("WELCOME10")).thenReturn(Optional.of(coupon));
        when(repository.save(any(Coupon.class))).thenReturn(coupon);

        CouponApplyResponse result = couponService.apply(request);

        assertThat(result.discountAmount()).isEqualByComparingTo("50.00");
        assertThat(result.finalAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void deactivate_shouldDeactivateCouponSuccessfully() {
        when(repository.findByCode("WELCOME10")).thenReturn(Optional.of(coupon));
        when(repository.save(any(Coupon.class))).thenReturn(coupon);
        when(mapper.toResponse(any(Coupon.class))).thenReturn(response);

        couponService.deactivate("WELCOME10");

        assertThat(coupon.getActive()).isFalse();
        verify(repository).save(coupon);
    }
}