package com.foody.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceTest {

    private final PricingService pricing = new PricingService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pricing, "taxPercent", 5.0);
        ReflectionTestUtils.setField(pricing, "deliveryFee", 40.0);
        ReflectionTestUtils.setField(pricing, "freeDeliveryAbove", 499.0);
    }

    @Test
    void smallOrderPaysDeliveryFeeAndTaxOnDiscountedAmount() {
        PricingService.Breakdown b = pricing.calculate(300, 100);   // taxable 200 -> tax 10, fee 40

        assertEquals(10.0, b.tax());
        assertEquals(40.0, b.deliveryFee());
        assertEquals(250.0, b.total());
    }

    @Test
    void largeOrderGetsFreeDelivery() {
        PricingService.Breakdown b = pricing.calculate(600, 0);     // tax 30, no fee

        assertEquals(0.0, b.deliveryFee());
        assertEquals(630.0, b.total());
    }
}
