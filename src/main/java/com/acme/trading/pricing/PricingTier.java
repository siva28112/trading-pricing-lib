package com.acme.trading.pricing;

import java.math.BigDecimal;

/**
 * Value object representing a pricing tier configuration.
 */
public record PricingTier(
        String name,
        BigDecimal basisPoints,
        BigDecimal volumeThreshold
) {}
