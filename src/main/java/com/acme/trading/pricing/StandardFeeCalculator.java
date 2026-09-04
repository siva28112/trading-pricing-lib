package com.acme.trading.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Standard flat-rate fee calculator for simple pricing scenarios.
 */
public class StandardFeeCalculator implements FeeCalculator {

    private final BigDecimal basisPoints;

    public StandardFeeCalculator(BigDecimal basisPoints) {
        this.basisPoints = basisPoints;
    }

    @Override
    public BigDecimal calculateFee(BigDecimal notionalValue, String accountTier) {
        if (notionalValue == null || notionalValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return notionalValue.multiply(basisPoints).setScale(2, RoundingMode.HALF_UP);
    }
}
