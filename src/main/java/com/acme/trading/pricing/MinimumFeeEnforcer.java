package com.acme.trading.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Minimum fee floor applied after tiered calculation for micro-orders.
 */
public class MinimumFeeEnforcer {

    private final BigDecimal minimumFee;

    public MinimumFeeEnforcer(BigDecimal minimumFee) {
        this.minimumFee = minimumFee;
    }

    public BigDecimal enforce(BigDecimal calculatedFee) {
        if (calculatedFee == null || calculatedFee.compareTo(minimumFee) < 0) {
            return minimumFee.setScale(2, RoundingMode.HALF_UP);
        }
        return calculatedFee.setScale(2, RoundingMode.HALF_UP);
    }
}
