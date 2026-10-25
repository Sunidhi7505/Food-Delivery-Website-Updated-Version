package com.foody.service;

import com.foody.dto.CouponValidationResponse;
import com.foody.model.Coupon;
import com.foody.repository.CouponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock CouponRepository couponRepository;
    @InjectMocks CouponService couponService;

    private Coupon coupon(String type, double value, double min, double max) {
        Coupon c = new Coupon();
        c.setCode("SAVE");
        c.setDiscountType(type);
        c.setDiscountValue(value);
        c.setMinOrderAmount(min);
        c.setMaxDiscountAmount(max);
        c.setActive(true);
        return c;
    }

    @Test
    void percentDiscountIsCappedByMaxDiscount() {
        when(couponRepository.findByCodeIgnoreCase("SAVE"))
                .thenReturn(Optional.of(coupon("PERCENT", 20, 0, 50)));

        CouponValidationResponse r = couponService.validate("SAVE", 1000);

        assertTrue(r.isValid());
        assertEquals(50, r.getDiscountAmount());
    }

    @Test
    void flatDiscountNeverExceedsSubtotal() {
        when(couponRepository.findByCodeIgnoreCase("SAVE"))
                .thenReturn(Optional.of(coupon("FLAT", 500, 0, 0)));

        CouponValidationResponse r = couponService.validate("SAVE", 300);

        assertEquals(300, r.getDiscountAmount());
    }

    @Test
    void expiredCouponIsRejected() {
        Coupon c = coupon("PERCENT", 10, 0, 0);
        c.setExpiryDate(LocalDate.now().minusDays(1));
        when(couponRepository.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.of(c));

        CouponValidationResponse r = couponService.validate("SAVE", 500);

        assertFalse(r.isValid());
        assertEquals("Coupon expired", r.getMessage());
    }

    @Test
    void couponBelowMinimumOrderIsRejected() {
        when(couponRepository.findByCodeIgnoreCase("SAVE"))
                .thenReturn(Optional.of(coupon("PERCENT", 10, 400, 0)));

        CouponValidationResponse r = couponService.validate("SAVE", 150);

        assertFalse(r.isValid());
        assertEquals("Add items worth Rs 250 more", r.getMessage());
    }
}
