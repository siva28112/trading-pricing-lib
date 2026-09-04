package com.acme.trading.pricing;

import java.math.BigDecimal;

/**
 * Calculates trading fees for a given notional value.
 */
public interface FeeCalculator {

    BigDecimal calculateFee(BigDecimal notionalValue, String accountTier);
}
