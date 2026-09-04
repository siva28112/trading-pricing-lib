package com.acme.trading.pricing;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Enriched price quote including fees and FX-adjusted notional.
 */
public record PriceQuote(
        String symbol,
        BigDecimal midPrice,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal estimatedFee,
        BigDecimal notionalUsd,
        String currency,
        Instant timestamp
) {}
