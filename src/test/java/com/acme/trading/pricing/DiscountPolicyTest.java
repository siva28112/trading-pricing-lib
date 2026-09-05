package com.acme.trading.pricing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscountPolicyTest {

    private static BigDecimal usd(String amount) {
        return new BigDecimal(amount);
    }

    @Nested
    class Institutional {

        private final InstitutionalDiscountPolicy policy = new InstitutionalDiscountPolicy();

        @Test
        void institutionalTierPaysHalf() {
            assertEquals(usd("50.00"), policy.applyDiscount(usd("100"), "INSTITUTIONAL"));
        }

        @Test
        void proTierPaysThreeQuarters() {
            assertEquals(usd("75.00"), policy.applyDiscount(usd("100"), "PRO"));
        }

        @ParameterizedTest(name = "tier [{0}] receives no discount")
        @NullSource
        @ValueSource(strings = {"RETAIL", "retail", "GOLD", "", "  "})
        void everyOtherTierPaysFull(String tier) {
            assertEquals(usd("100.00"), policy.applyDiscount(usd("100"), tier));
        }

        @ParameterizedTest(name = "tier [{0}] normalises to a 50% discount")
        @ValueSource(strings = {"INSTITUTIONAL", "institutional", " Institutional "})
        void tierIsTrimmedAndUpperCased(String tier) {
            assertEquals(usd("50.00"), policy.applyDiscount(usd("100"), tier));
        }

        @Test
        void nullBaseFeeBecomesZero() {
            assertEquals(usd("0.00"), policy.applyDiscount(null, "INSTITUTIONAL"));
        }

        @ParameterizedTest(name = "{0} of {1} rounds half up to {2}")
        @CsvSource({
                // 2.01 * 0.75 = 1.5075 -> 1.51
                "PRO,           2.01, 1.51",
                // 0.03 * 0.50 = 0.015 -> 0.02
                "INSTITUTIONAL, 0.03, 0.02",
                // 0.01 * 0.50 = 0.005 -> 0.01
                "INSTITUTIONAL, 0.01, 0.01"
        })
        void discountsRoundHalfUpToCents(String tier, String baseFee, String expected) {
            assertEquals(usd(expected), policy.applyDiscount(usd(baseFee), tier));
        }

        @Test
        void aZeroFeeStaysZeroForEveryTier() {
            assertEquals(usd("0.00"), policy.applyDiscount(usd("0"), "INSTITUTIONAL"));
            assertEquals(usd("0.00"), policy.applyDiscount(usd("0"), "PRO"));
            assertEquals(usd("0.00"), policy.applyDiscount(usd("0"), "RETAIL"));
        }
    }

    @Nested
    class NoDiscount {

        private final NoDiscountPolicy policy = new NoDiscountPolicy();

        @ParameterizedTest(name = "tier [{0}] is ignored entirely")
        @NullSource
        @ValueSource(strings = {"INSTITUTIONAL", "PRO", "RETAIL", "ANYTHING"})
        void tierIsIgnored(String tier) {
            assertEquals(usd("100.00"), policy.applyDiscount(usd("100"), tier));
        }

        @Test
        void nullBaseFeeBecomesZero() {
            assertEquals(usd("0.00"), policy.applyDiscount(null, "INSTITUTIONAL"));
        }

        @Test
        void feeIsRescaledToCentsButNotAltered() {
            // 12.345 -> 12.35 by rounding only; no discount is applied.
            assertEquals(usd("12.35"), policy.applyDiscount(usd("12.345"), "PRO"));
            assertEquals(2, policy.applyDiscount(usd("12"), "PRO").scale());
        }
    }
}
