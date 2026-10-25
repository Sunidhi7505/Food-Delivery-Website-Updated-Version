package com.foody.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Single source of truth for delivery fee and GST so cart, order and invoice always agree. */
@Service
public class PricingService {

    public record Breakdown(double subtotal, double discount, double tax, double deliveryFee, double total) {}

    @Value("${foody.pricing.tax-percent:5}")
    private double taxPercent;

    @Value("${foody.pricing.delivery-fee:40}")
    private double deliveryFee;

    @Value("${foody.pricing.free-delivery-above:499}")
    private double freeDeliveryAbove;

    public Breakdown calculate(double subtotal, double discount) {
        double taxable = Math.max(subtotal - discount, 0);
        double tax = round2(taxable * taxPercent / 100);
        double delivery = (taxable <= 0 || taxable >= freeDeliveryAbove) ? 0 : deliveryFee;
        return new Breakdown(subtotal, discount, tax, delivery, round2(taxable + tax + delivery));
    }

    public Map<String, Object> getConfig() {
        return Map.of("taxPercent", taxPercent, "deliveryFee", deliveryFee, "freeDeliveryAbove", freeDeliveryAbove);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
