package com.acme.trading.pricing;

import com.acme.trading.dto.Quote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Central pricing engine combining quotes, fees, discounts, and FX conversion.
 */
public class PricingEngine {

    private final FeeCalculator feeCalculator;
    private final DiscountPolicy discountPolicy;
    private final FxConversionService fxConversionService;

    public PricingEngine(FeeCalculator feeCalculator,
                         DiscountPolicy discountPolicy,
                         FxConversionService fxConversionService) {
        this.feeCalculator = feeCalculator;
        this.discountPolicy = discountPolicy;
        this.fxConversionService = fxConversionService;
    }

    public static PricingEngine defaultEngine() {
        return new PricingEngine(
                new TieredFeeCalculator(),
                new InstitutionalDiscountPolicy(),
                new FxConversionService()
        );
    }

    public PriceQuote priceOrder(Quote marketQuote, BigDecimal quantity, String accountTier, String currency) {
        BigDecimal mid = marketQuote.last() != null
                ? marketQuote.last()
                : marketQuote.bid().add(marketQuote.ask()).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);

        BigDecimal notional = mid.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        BigDecimal baseFee = feeCalculator.calculateFee(notional, accountTier);
        BigDecimal finalFee = discountPolicy.applyDiscount(baseFee, accountTier);
        BigDecimal notionalUsd = fxConversionService.toUsd(notional, currency);

        return new PriceQuote(
                marketQuote.symbol(),
                mid,
                marketQuote.bid(),
                marketQuote.ask(),
                finalFee,
                notionalUsd,
                currency == null ? "USD" : currency,
                Instant.now()
        );
    }

    public BigDecimal estimateFee(BigDecimal notionalValue, String accountTier) {
        BigDecimal baseFee = feeCalculator.calculateFee(notionalValue, accountTier);
        return discountPolicy.applyDiscount(baseFee, accountTier);
    }
}
