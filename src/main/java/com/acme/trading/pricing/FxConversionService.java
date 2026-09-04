package com.acme.trading.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Converts foreign currency amounts to USD using static sample rates.
 */
public class FxConversionService {

    private static final Map<String, BigDecimal> USD_RATES = Map.of(
            "USD", BigDecimal.ONE,
            "EUR", new BigDecimal("1.08"),
            "GBP", new BigDecimal("1.27"),
            "JPY", new BigDecimal("0.0067")
    );

    public BigDecimal toUsd(BigDecimal amount, String currency) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        String code = currency == null ? "USD" : currency.trim().toUpperCase();
        BigDecimal rate = USD_RATES.getOrDefault(code, BigDecimal.ONE);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getRate(String currency) {
        String code = currency == null ? "USD" : currency.trim().toUpperCase();
        return USD_RATES.getOrDefault(code, BigDecimal.ONE);
    }
}
