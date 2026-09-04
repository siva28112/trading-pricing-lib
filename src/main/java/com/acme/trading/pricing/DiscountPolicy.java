package com.acme.trading.pricing;

import java.math.BigDecimal;

/**
 * Applies account-level or promotional discounts to base fees.
 */
public interface DiscountPolicy {

    BigDecimal applyDiscount(BigDecimal baseFee, String accountTier);
}
