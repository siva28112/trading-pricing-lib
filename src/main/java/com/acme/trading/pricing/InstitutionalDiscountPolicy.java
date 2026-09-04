package com.acme.trading.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Institutional discount policy: 50% off base fees for INSTITUTIONAL tier,
 * 25% off for PRO tier. Retail accounts receive no discount.
 */
public class InstitutionalDiscountPolicy implements DiscountPolicy {

    private static final BigDecimal INSTITUTIONAL_DISCOUNT = new BigDecimal("0.50");
    private static final BigDecimal PRO_DISCOUNT = new BigDecimal("0.25");

    @Override
    public BigDecimal applyDiscount(BigDecimal baseFee, String accountTier) {
        if (baseFee == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        String tier = accountTier == null ? "RETAIL" : accountTier.trim().toUpperCase();
        BigDecimal multiplier = switch (tier) {
            case "INSTITUTIONAL" -> BigDecimal.ONE.subtract(INSTITUTIONAL_DISCOUNT);
            case "PRO" -> BigDecimal.ONE.subtract(PRO_DISCOUNT);
            default -> BigDecimal.ONE;
        };
        return baseFee.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
