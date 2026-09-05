package com.acme.trading.pricing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FxConversionServiceTest {

    private final FxConversionService service = new FxConversionService();

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }

    @ParameterizedTest(name = "{0} {1} converts to {2} USD")
    @CsvSource({
            "USD, 100,  100.00",
            "EUR, 100,  108.00",   // 100 * 1.08
            "GBP, 100,  127.00",   // 100 * 1.27
            "JPY, 1000, 6.70"      // 1000 * 0.0067 = 6.7
    })
    void knownCurrenciesConvertAtTheirRate(String currency, String value, String expected) {
        assertEquals(amount(expected), service.toUsd(amount(value), currency));
    }

    @ParameterizedTest(name = "currency [{0}] is normalised to EUR")
    @ValueSource(strings = {"EUR", "eur", " eur ", "Eur", "\tEUR\n"})
    void currencyCodeIsTrimmedAndUpperCased(String currency) {
        assertEquals(amount("108.00"), service.toUsd(amount("100"), currency));
    }

    @Test
    void nullCurrencyIsTreatedAsUsd() {
        assertEquals(amount("100.00"), service.toUsd(amount("100"), null));
        assertEquals(BigDecimal.ONE, service.getRate(null));
    }

    @ParameterizedTest(name = "unknown currency [{0}] converts one to one")
    @ValueSource(strings = {"XYZ", "CHF", "AUD", "", "  "})
    void unknownCurrenciesSilentlyConvertOneToOne(String currency) {
        // Documents current behaviour: getOrDefault falls back to a rate of 1 rather than
        // rejecting the code, so an unsupported currency is converted, not refused.
        assertEquals(amount("100.00"), service.toUsd(amount("100"), currency));
        assertEquals(BigDecimal.ONE, service.getRate(currency));
    }

    @Test
    void nullAmountBecomesZero() {
        assertEquals(amount("0.00"), service.toUsd(null, "EUR"));
        assertEquals(2, service.toUsd(null, "EUR").scale());
    }

    @Test
    void negativeAmountsConvertWithoutSpecialCasing() {
        assertEquals(amount("-108.00"), service.toUsd(amount("-100"), "EUR"));
    }

    @Test
    void conversionRoundsHalfUpToCents() {
        // 1.005 * 1 = 1.005 -> 1.01
        assertEquals(amount("1.01"), service.toUsd(amount("1.005"), "USD"));
        // 333 * 0.0067 = 2.2311 -> 2.23
        assertEquals(amount("2.23"), service.toUsd(amount("333"), "JPY"));
    }

    @ParameterizedTest(name = "rate for {0} is {1}")
    @CsvSource({"USD, 1", "EUR, 1.08", "GBP, 1.27", "JPY, 0.0067"})
    void ratesAreExposedUnscaled(String currency, String expected) {
        assertEquals(new BigDecimal(expected), service.getRate(currency));
    }

    @Test
    void toUsdAgreesWithGetRate() {
        BigDecimal value = amount("250");
        for (String currency : new String[]{"USD", "EUR", "GBP", "JPY", "XYZ"}) {
            assertEquals(
                    value.multiply(service.getRate(currency)).setScale(2, java.math.RoundingMode.HALF_UP),
                    service.toUsd(value, currency),
                    "toUsd disagrees with getRate for " + currency);
        }
    }
}
