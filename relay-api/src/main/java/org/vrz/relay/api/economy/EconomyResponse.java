package org.vrz.relay.api.economy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Result data transfer object returned by economy mutation operations (deposit, withdraw, transfer).
 */
public record EconomyResponse(
        @NotNull UUID transactionId,
        @NotNull Status status,
        @NotNull BigDecimal amount,
        @NotNull BigDecimal balanceAfter,
        @NotNull Currency currency,
        @Nullable String errorMessage
) {

    /**
     * Checks if the transaction completed successfully.
     *
     * @return true if status is SUCCESS
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Creates a successful economy response.
     */
    @NotNull
    public static EconomyResponse success(@NotNull BigDecimal amount,
                                          @NotNull BigDecimal balanceAfter,
                                          @NotNull Currency currency) {
        return new EconomyResponse(UUID.randomUUID(), Status.SUCCESS, amount, balanceAfter, currency, null);
    }

    /**
     * Creates a failed economy response.
     */
    @NotNull
    public static EconomyResponse failure(@NotNull Status status,
                                          @NotNull BigDecimal amount,
                                          @NotNull BigDecimal balanceAfter,
                                          @NotNull Currency currency,
                                          @NotNull String errorMessage) {
        return new EconomyResponse(UUID.randomUUID(), status, amount, balanceAfter, currency, errorMessage);
    }

    /**
     * Status codes describing the outcome of an economy transaction.
     */
    public enum Status {
        /**
         * Transaction executed and persisted successfully.
         */
        SUCCESS,

        /**
         * The account does not hold sufficient balance to withdraw/transfer this amount.
         */
        INSUFFICIENT_FUNDS,

        /**
         * The targeted account does not exist or has not been initialized.
         */
        ACCOUNT_NOT_FOUND,

        /**
         * The requested currency is not registered or supported by this economy provider.
         */
        CURRENCY_NOT_SUPPORTED,

        /**
         * The provided amount was zero, negative, or invalid for this currency.
         */
        INVALID_AMOUNT,

        /**
         * The transaction was aborted or cancelled by an event or safety filter.
         */
        CANCELLED,

        /**
         * An unexpected error or database failure occurred during processing.
         */
        ERROR
    }
}
