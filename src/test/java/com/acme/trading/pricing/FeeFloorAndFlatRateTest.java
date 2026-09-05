package com.acme.trading.pricing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeeFloorAndFlatRateTest {

    private static BigDecimal usd(String amount) {
        return new BigDecimal(amount);
    }

    @Nested
    class MinimumFee {

        private final MinimumFeeEnforcer enforcer = new MinimumFeeEnforcer(usd("1.00"));

        @Test
        void feeBelowTheFloorIsRaisedToTheFloor() {
            assertEquals(usd("1.00"), enforcer.enforce(usd("0.50")));
            assertEquals(usd("1.00"), enforcer.enforce(usd("0")));
        }

        @Test
        void feeExactlyAtTheFloorIsKept() {
            // The comparison is `< minimumFee`, so the boundary passes through unchanged.
            assertEquals(usd("1.00"), enforcer.enforce(usd("1.00")));
        }

        @Test
        void feeAboveTheFloorIsKept() {
            assertEquals(usd("2.50"), enforcer.enforce(usd("2.50")));
        }

        @Test
        void nullFeeFallsBackToTheFloor() {
            assertEquals(usd("1.00"), enforcer.enforce(null));
        }

        @Test
        void feeAboveTheFloorIsRoundedHalfUpToCents() {
            assertEquals(usd("2.35"), enforcer.enforce(usd("2.345")));
        }

        @Test
        void scaleOfTheFloorDoesNotAffectTheComparison() {
            MinimumFeeEnforcer unscaled = new MinimumFeeEnforcer(usd("1"));
            assertEquals(usd("1.00"), unscaled.enforce(usd("1.00")));
            assertEquals(usd("1.00"), unscaled.enforce(usd("0.99")));
        }

        @Test
        void aNegativeFeeIsRaisedToTheFloor() {
            assertEquals(usd("1.00"), enforcer.enforce(usd("-5")));
        }
    }

    @Nested
    class StandardFlatRate {

        private final StandardFeeCalculator calculator = new StandardFeeCalculator(usd("0.0025"));

        @Test
        void feeIsNotionalTimesBasisPoints() {
            // 10000 * 0.0025 = 25.00
            assertEquals(usd("25.00"), calculator.calculateFee(usd("10000"), "RETAIL"));
        }

        @ParameterizedTest(name = "tier [{0}] does not change a flat rate")
        @NullSource
        @ValueSource(strings = {"RETAIL", "PRO", "INSTITUTIONAL", "GOLD"})
        void tierIsIgnored(String tier) {
            assertEquals(usd("25.00"), calculator.calculateFee(usd("10000"), tier));
        }

        @ParameterizedTest(name = "notional {0} yields no fee")
        @ValueSource(strings = {"0", "-1"})
        void nonPositiveNotionalYieldsZeroFee(String notional) {
            assertEquals(usd("0.00"), calculator.calculateFee(usd(notional), "RETAIL"));
        }

        @Test
        void nullNotionalYieldsZeroFee() {
            assertEquals(usd("0.00"), calculator.calculateFee(null, "RETAIL"));
        }

        @Test
        void feeRoundsHalfUpToCents() {
            // 1002 * 0.0025 = 2.505 -> 2.51
            assertEquals(usd("2.51"), calculator.calculateFee(usd("1002"), "RETAIL"));
        }
    }
}
