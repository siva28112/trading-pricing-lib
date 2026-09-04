package com.acme.trading.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * No-op discount policy for environments that do not apply fee reductions.
 */
public class NoDiscountPolicy implements DiscountPolicy {

    @Override
    public BigDecimal applyDiscount(BigDecimal baseFee, String accountTier) {
        if (baseFee == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return baseFee.setScale(2, RoundingMode.HALF_UP);
    }
}
