package org.vrz.relay.api.economy;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Descriptor representing a distinct currency denomination in the Relay Economy ecosystem.
 * <p>
 * Supports both fiat-style fractional currencies (e.g. Coins with 2 decimals)
 * and whole integer currencies (e.g. Diamonds, Tokens, Seasonal Points).
 */
public record Currency(
        @NotNull String id,
        @NotNull String displayName,
        @NotNull String pluralName,
        @NotNull String symbol,
        int fractionalDigits
) {

    public Currency {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Currency ID cannot be null or blank");
        }
        if (fractionalDigits < 0) {
            throw new IllegalArgumentException("Fractional digits must be non-negative");
        }
    }

    /**
     * Formats an amount with this currency's symbol, decimals, and naming conventions.
     *
     * @param amount the numeric amount
     * @return formatted string (e.g. "$1,250.50" or "500 Tokens")
     */
    @NotNull
    public String format(@NotNull BigDecimal amount) {
        BigDecimal scaled = amount.setScale(fractionalDigits, RoundingMode.HALF_UP);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');

        DecimalFormat format = new DecimalFormat();
        format.setDecimalFormatSymbols(symbols);
        format.setMinimumFractionDigits(fractionalDigits);
        format.setMaximumFractionDigits(fractionalDigits);
        format.setGroupingUsed(true);

        String numberStr = format.format(scaled);
        if (symbol != null && !symbol.isBlank()) {
            return symbol + numberStr;
        }
        return numberStr + " " + (amount.compareTo(BigDecimal.ONE) == 0 ? displayName : pluralName);
    }

    /**
     * Standard helper to create a whole-number token currency.
     *
     * @param id          currency ID
     * @param displayName singular name
     * @param pluralName  plural name
     * @param symbol      symbol or prefix
     * @return new Currency instance
     */
    @NotNull
    public static Currency integer(@NotNull String id, @NotNull String displayName, @NotNull String pluralName, @NotNull String symbol) {
        return new Currency(id.toLowerCase(Locale.ROOT), displayName, pluralName, symbol, 0);
    }

    /**
     * Standard helper to create a decimal fractional currency (2 decimal places).
     *
     * @param id          currency ID
     * @param displayName singular name
     * @param pluralName  plural name
     * @param symbol      symbol or prefix
     * @return new Currency instance
     */
    @NotNull
    public static Currency decimal(@NotNull String id, @NotNull String displayName, @NotNull String pluralName, @NotNull String symbol) {
        return new Currency(id.toLowerCase(Locale.ROOT), displayName, pluralName, symbol, 2);
    }
}
