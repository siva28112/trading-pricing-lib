package com.acme.trading.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tiered fee schedule based on daily notional volume.
 *
 * <p>Business rules:
 * <ul>
 *   <li>Retail: 10 bps on first $10k, 5 bps above</li>
 *   <li>Pro: flat 3 bps</li>
 *   <li>Institutional: flat 1 bp (see {@link InstitutionalDiscountPolicy})</li>
 * </ul>
 */
public class TieredFeeCalculator implements FeeCalculator {

    private static final BigDecimal RETAIL_THRESHOLD = new BigDecimal("10000");
    private static final BigDecimal RETAIL_LOW_BPS = new BigDecimal("0.0010");
    private static final BigDecimal RETAIL_HIGH_BPS = new BigDecimal("0.0005");
    private static final BigDecimal PRO_BPS = new BigDecimal("0.0003");
    private static final BigDecimal INSTITUTIONAL_BPS = new BigDecimal("0.0001");

    @Override
    public BigDecimal calculateFee(BigDecimal notionalValue, String accountTier) {
        if (notionalValue == null || notionalValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal fee = switch (normalizeTier(accountTier)) {
            case "INSTITUTIONAL" -> notionalValue.multiply(INSTITUTIONAL_BPS);
            case "PRO" -> notionalValue.multiply(PRO_BPS);
            default -> calculateRetailFee(notionalValue);
        };
        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRetailFee(BigDecimal notionalValue) {
        if (notionalValue.compareTo(RETAIL_THRESHOLD) <= 0) {
            return notionalValue.multiply(RETAIL_LOW_BPS);
        }
        BigDecimal lowTier = RETAIL_THRESHOLD.multiply(RETAIL_LOW_BPS);
        BigDecimal highTier = notionalValue.subtract(RETAIL_THRESHOLD).multiply(RETAIL_HIGH_BPS);
        return lowTier.add(highTier);
    }

    private String normalizeTier(String accountTier) {
        if (accountTier == null) {
            return "RETAIL";
        }
        return accountTier.trim().toUpperCase();
    }
}
