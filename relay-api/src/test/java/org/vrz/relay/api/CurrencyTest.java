package org.vrz.relay.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.vrz.relay.api.economy.Currency;
import org.vrz.relay.api.economy.EconomyResponse;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyTest {

    @Test
    @DisplayName("Decimal currency formats properly with symbol and grouping")
    void testDecimalCurrencyFormat() {
        Currency dollar = Currency.decimal("usd", "Dollar", "Dollars", "$");
        assertEquals("usd", dollar.id());
        assertEquals(2, dollar.fractionalDigits());

        String formatted = dollar.format(new BigDecimal("1234567.891"));
        assertEquals("$1,234,567.89", formatted);
    }

    @Test
    @DisplayName("Integer currency formats without decimals")
    void testIntegerCurrencyFormat() {
        Currency token = Currency.integer("token", "Token", "Tokens", "");
        assertEquals("token", token.id());
        assertEquals(0, token.fractionalDigits());

        String formattedSingle = token.format(BigDecimal.ONE);
        assertEquals("1 Token", formattedSingle);

        String formattedPlural = token.format(new BigDecimal("5000"));
        assertEquals("5,000 Tokens", formattedPlural);
    }

    @Test
    @DisplayName("EconomyResponse helpers function as expected")
    void testEconomyResponse() {
        Currency gold = Currency.integer("gold", "Gold", "Gold", "G");
        EconomyResponse success = EconomyResponse.success(BigDecimal.TEN, new BigDecimal("100"), gold);

        assertTrue(success.isSuccess());
        assertEquals(EconomyResponse.Status.SUCCESS, success.status());
        assertNull(success.errorMessage());

        EconomyResponse failure = EconomyResponse.failure(
                EconomyResponse.Status.INSUFFICIENT_FUNDS,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                gold,
                "Not enough gold"
        );

        assertFalse(failure.isSuccess());
        assertEquals(EconomyResponse.Status.INSUFFICIENT_FUNDS, failure.status());
        assertEquals("Not enough gold", failure.errorMessage());
    }
}
