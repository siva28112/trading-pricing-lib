package com.acme.trading.pricing;

import com.acme.trading.dto.Quote;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingEngineTest {

    private static final Instant AS_OF = Instant.parse("2026-01-15T10:00:00Z");

    private final PricingEngine engine = PricingEngine.defaultEngine();

    private static BigDecimal usd(String amount) {
        return new BigDecimal(amount);
    }

    private static Quote quote(String bid, String ask, String last) {
        return new Quote("AAPL", usd(bid), usd(ask), last == null ? null : usd(last),
                usd("1000000"), AS_OF);
    }

    @Test
    void midComesFromLastTradeWhenPresent() {
        PriceQuote priced = engine.priceOrder(quote("100.00", "102.00", "101.50"), usd("10"), "RETAIL", "USD");

        // last is used verbatim, with its own scale -- it is not re-scaled to 4dp.
        assertEquals(usd("101.50"), priced.midPrice());
    }

    @Test
    void midFallsBackToTheBidAskMidpointWhenThereIsNoLastTrade() {
        PriceQuote priced = engine.priceOrder(quote("100.00", "102.00", null), usd("10"), "RETAIL", "USD");

        // (100.00 + 102.00) / 2 at scale 4
        assertEquals(usd("101.0000"), priced.midPrice());
    }

    @Test
    void midpointFallbackRoundsHalfUpAtFourDecimals() {
        // (100.00 + 100.01) / 2 = 100.005 -> 100.0050 at scale 4
        PriceQuote priced = engine.priceOrder(quote("100.00", "100.01", null), usd("1"), "RETAIL", "USD");
        assertEquals(usd("100.0050"), priced.midPrice());
    }

    @Test
    void feeIsCalculatedOnMidTimesQuantity() {
        // mid 101.50 * 10 = 1015.00 notional; retail is under the 10k threshold,
        // so 1015.00 * 0.0010 = 1.015 -> 1.02 (half up).
        PriceQuote priced = engine.priceOrder(quote("100.00", "102.00", "101.50"), usd("10"), "RETAIL", "USD");
        assertEquals(usd("1.02"), priced.estimatedFee());
    }

    @Test
    void notionalIsConvertedToUsdUsingTheQuoteCurrency() {
        // mid 101.50 * 10 = 1015.00 notional; 1015.00 * 1.08 = 1096.20
        PriceQuote priced = engine.priceOrder(quote("100.00", "102.00", "101.50"), usd("10"), "RETAIL", "EUR");
        assertEquals(usd("1096.20"), priced.notionalUsd());
        assertEquals("EUR", priced.currency());
    }

    @Test
    void nullCurrencyIsReportedAndConvertedAsUsd() {
        PriceQuote priced = engine.priceOrder(quote("100.00", "102.00", "101.50"), usd("10"), "RETAIL", null);

        assertEquals("USD", priced.currency());
        assertEquals(usd("1015.00"), priced.notionalUsd());
    }

    @Test
    void quoteFieldsArePassedThrough() {
        Quote market = quote("100.00", "102.00", "101.50");
        PriceQuote priced = engine.priceOrder(market, usd("10"), "RETAIL", "USD");

        assertEquals("AAPL", priced.symbol());
        assertEquals(market.bid(), priced.bid());
        assertEquals(market.ask(), priced.ask());
        assertNotNull(priced.timestamp());
    }

    @Test
    void pricedAtIsStampedAtPricingTimeNotFromTheQuote() {
        Instant before = Instant.now();
        PriceQuote priced = engine.priceOrder(quote("100.00", "102.00", "101.50"), usd("10"), "RETAIL", "USD");
        Instant after = Instant.now();

        assertTrue(!priced.timestamp().isBefore(before) && !priced.timestamp().isAfter(after),
                "PriceQuote.timestamp should be the pricing instant, not the market quote's " + AS_OF);
    }

    @ParameterizedTest(name = "estimateFee({1}, {0}) = {2}")
    @CsvSource({
            // Retail 20000: 10000*0.0010 + 10000*0.0005 = 15.00, then no discount.
            "RETAIL,        20000, 15.00",
            // Pro 20000: 20000*0.0003 = 6.00, then a 25% discount = 4.50.
            "PRO,           20000, 4.50",
            // Institutional 20000: 20000*0.0001 = 2.00, then a 50% discount = 1.00.
            "INSTITUTIONAL, 20000, 1.00"
    })
    void estimateFeeAppliesTheTierScheduleThenTheDiscount(String tier, String notional, String expected) {
        assertEquals(usd(expected), engine.estimateFee(usd(notional), tier));
    }

    @Test
    void institutionalIsDiscountedTwiceByTheDefaultEngine() {
        // Recorded, not endorsed. TieredFeeCalculator already charges institutional a
        // flat 1 bp, and InstitutionalDiscountPolicy then halves that fee again, so the
        // effective institutional rate is 0.5 bp rather than the 1 bp the schedule states.
        BigDecimal scheduleRate = new TieredFeeCalculator().calculateFee(usd("20000"), "INSTITUTIONAL");
        BigDecimal effective = engine.estimateFee(usd("20000"), "INSTITUTIONAL");

        assertEquals(usd("2.00"), scheduleRate);
        assertEquals(usd("1.00"), effective);
    }

    @Test
    void estimateFeeAndPriceOrderAgreeOnTheSameNotional() {
        // mid 100.00 * 200 = 20000.00 notional, so both paths must produce the same fee.
        PriceQuote priced = engine.priceOrder(quote("99.00", "101.00", "100.00"), usd("200"), "PRO", "USD");
        assertEquals(engine.estimateFee(usd("20000.00"), "PRO"), priced.estimatedFee());
    }

    @Test
    void collaboratorsAreComposableForNonDefaultConfigurations() {
        PricingEngine flat = new PricingEngine(
                new StandardFeeCalculator(usd("0.0025")),
                new NoDiscountPolicy(),
                new FxConversionService());

        // 10000 * 0.0025 = 25.00, and NoDiscountPolicy leaves it alone.
        assertEquals(usd("25.00"), flat.estimateFee(usd("10000"), "INSTITUTIONAL"));
    }

    @Test
    void defaultEngineBuildsAFreshInstanceEachCall() {
        PricingEngine first = PricingEngine.defaultEngine();
        PricingEngine second = PricingEngine.defaultEngine();

        assertNotNull(first);
        assertNotNull(second);
        assertTrue(first != second, "defaultEngine should not hand out a shared singleton");
        assertSame(first.estimateFee(usd("5000"), "PRO").getClass(),
                second.estimateFee(usd("5000"), "PRO").getClass());
    }
}
