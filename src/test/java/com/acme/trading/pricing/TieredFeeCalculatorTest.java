package com.acme.trading.pricing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Retail is 10 bps on the first $10k and 5 bps above; Pro is a flat 3 bps;
 * Institutional a flat 1 bp. Every expected value below is computed by hand from
 * those rules rather than copied from a run.
 */
class TieredFeeCalculatorTest {

    private final TieredFeeCalculator calculator = new TieredFeeCalculator();

    private static BigDecimal usd(String amount) {
        return new BigDecimal(amount);
    }

    @Test
    @DisplayName("retail below the threshold pays a flat 10 bps")
    void retailBelowThreshold() {
        // 5000 * 0.0010 = 5.00
        assertEquals(usd("5.00"), calculator.calculateFee(usd("5000"), "RETAIL"));
    }

    @Test
    @DisplayName("retail exactly at the $10k threshold is still all 10 bps")
    void retailAtThreshold() {
        // 10000 * 0.0010 = 10.00
        //
        // Note this does NOT pin the `<=` in the branch condition. The schedule is
        // continuous at the threshold -- the high-tier arm computes
        // 10000*0.0010 + 0*0.0005, which is the same 10.00 -- so flipping `<=` to `<`
        // changes no output anywhere and no test can detect it. Verified by mutation:
        // that edit leaves all 105 tests green. The value is still worth pinning; the
        // branch simply is not observable from behaviour.
        assertEquals(usd("10.00"), calculator.calculateFee(usd("10000"), "RETAIL"));
    }

    @Test
    @DisplayName("retail one cent above the threshold splits across both tiers")
    void retailJustAboveThreshold() {
        // 10000*0.0010 + 0.01*0.0005 = 10 + 0.000005 = 10.000005 -> 10.00
        assertEquals(usd("10.00"), calculator.calculateFee(usd("10000.01"), "RETAIL"));
    }

    @Test
    @DisplayName("retail well above the threshold pays 10 bps then 5 bps")
    void retailAboveThreshold() {
        // 10000*0.0010 + 10000*0.0005 = 10 + 5 = 15.00
        assertEquals(usd("15.00"), calculator.calculateFee(usd("20000"), "RETAIL"));
        // 10000*0.0010 + 90000*0.0005 = 10 + 45 = 55.00
        assertEquals(usd("55.00"), calculator.calculateFee(usd("100000"), "RETAIL"));
    }

    @Test
    @DisplayName("pro is a flat 3 bps with no threshold")
    void proIsFlat() {
        assertEquals(usd("6.00"), calculator.calculateFee(usd("20000"), "PRO"));
        assertEquals(usd("1.50"), calculator.calculateFee(usd("5000"), "PRO"));
    }

    @Test
    @DisplayName("institutional is a flat 1 bp with no threshold")
    void institutionalIsFlat() {
        assertEquals(usd("2.00"), calculator.calculateFee(usd("20000"), "INSTITUTIONAL"));
        assertEquals(usd("0.50"), calculator.calculateFee(usd("5000"), "INSTITUTIONAL"));
    }

    @ParameterizedTest(name = "tier [{0}] is normalised to PRO")
    @ValueSource(strings = {"PRO", "pro", " pro ", "Pro", "\tPRO\n"})
    void tierIsTrimmedAndUpperCased(String tier) {
        assertEquals(usd("6.00"), calculator.calculateFee(usd("20000"), tier));
    }

    @ParameterizedTest(name = "unrecognised tier [{0}] falls back to retail")
    @NullSource
    @ValueSource(strings = {"RETAIL", "GOLD", "", "   ", "institutional-plus"})
    void unrecognisedTiersFallBackToRetail(String tier) {
        // 20000 retail = 15.00. A null or unknown tier must never be cheaper than retail.
        assertEquals(usd("15.00"), calculator.calculateFee(usd("20000"), tier));
    }

    @ParameterizedTest(name = "notional {0} yields no fee")
    @ValueSource(strings = {"0", "0.00", "-1", "-10000"})
    void nonPositiveNotionalYieldsZeroFee(String notional) {
        assertEquals(usd("0.00"), calculator.calculateFee(usd(notional), "RETAIL"));
    }

    @Test
    void nullNotionalYieldsZeroFee() {
        assertEquals(usd("0.00"), calculator.calculateFee(null, "RETAIL"));
    }

    @ParameterizedTest(name = "{0} at {1} rounds half up to {2}")
    @CsvSource({
            // 3333 * 0.0010 = 3.333 -> 3.33
            "RETAIL,         3333,  3.33",
            // 3335 * 0.0010 = 3.335 -> 3.34 (half up, not half even)
            "RETAIL,         3335,  3.34",
            // 8350 * 0.0003 = 2.505 -> 2.51
            "PRO,            8350,  2.51",
            // 25050 * 0.0001 = 2.505 -> 2.51
            "INSTITUTIONAL,  25050, 2.51"
    })
    void feesRoundHalfUpToCents(String tier, String notional, String expected) {
        assertEquals(usd(expected), calculator.calculateFee(usd(notional), tier));
    }

    @Test
    void resultIsAlwaysScaledToCents() {
        assertEquals(2, calculator.calculateFee(usd("5000"), "RETAIL").scale());
        assertEquals(2, calculator.calculateFee(usd("0"), "RETAIL").scale());
        assertEquals(2, calculator.calculateFee(null, null).scale());
    }
}
