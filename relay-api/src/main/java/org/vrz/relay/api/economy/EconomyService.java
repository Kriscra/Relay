package org.vrz.relay.api.economy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Universal, asynchronous, and multi-currency economy service specification for Minecraft servers.
 * <p>
 * Replaces Vault's synchronous double-precision API with non-blocking {@link CompletableFuture},
 * lossless {@link BigDecimal} arithmetic, and native multi-currency support.
 */
public interface EconomyService {

    /**
     * Gets the primary/default currency used on this server.
     *
     * @return default currency descriptor
     */
    @NotNull
    Currency getDefaultCurrency();

    /**
     * Look up a registered currency by its unique string identifier (case-insensitive).
     *
     * @param currencyId the identifier (e.g. "gold", "token")
     * @return optional containing the currency descriptor, or empty if unsupported
     */
    @NotNull
    Optional<Currency> getCurrency(@NotNull String currencyId);

    /**
     * Returns an unmodifiable set of all currencies supported by this economy provider.
     *
     * @return supported currencies
     */
    @NotNull
    Set<Currency> getSupportedCurrencies();

    /**
     * Checks asynchronously whether an economy account exists for the given UUID.
     *
     * @param accountId player or bank account UUID
     * @return future yielding true if the account exists
     */
    @NotNull
    CompletableFuture<Boolean> hasAccount(@NotNull UUID accountId);

    /**
     * Creates an economy account for the given UUID if it doesn't already exist.
     *
     * @param accountId player or bank account UUID
     * @return future yielding true if created or already existed, false on failure
     */
    @NotNull
    CompletableFuture<Boolean> createAccount(@NotNull UUID accountId);

    /**
     * Retrieves the current balance for an account in the given currency.
     *
     * @param accountId account UUID
     * @param currency  target currency
     * @return future yielding the balance, or BigDecimal.ZERO if no account
     */
    @NotNull
    CompletableFuture<BigDecimal> getBalance(@NotNull UUID accountId, @NotNull Currency currency);

    /**
     * Retrieves the current balance for an account using the default currency.
     *
     * @param accountId account UUID
     * @return future yielding default currency balance
     */
    @NotNull
    default CompletableFuture<BigDecimal> getBalance(@NotNull UUID accountId) {
        return getBalance(accountId, getDefaultCurrency());
    }

    /**
     * Checks if the account holds at least the specified amount in the given currency.
     *
     * @param accountId account UUID
     * @param amount    amount to test
     * @param currency  target currency
     * @return future yielding true if balance >= amount
     */
    @NotNull
    CompletableFuture<Boolean> has(@NotNull UUID accountId, @NotNull BigDecimal amount, @NotNull Currency currency);

    /**
     * Checks if the account holds at least the specified amount in the default currency.
     *
     * @param accountId account UUID
     * @param amount    amount to test
     * @return future yielding true if balance >= amount
     */
    @NotNull
    default CompletableFuture<Boolean> has(@NotNull UUID accountId, @NotNull BigDecimal amount) {
        return has(accountId, amount, getDefaultCurrency());
    }

    /**
     * Deposits funds into the given account.
     *
     * @param accountId target account UUID
     * @param amount    positive amount to deposit
     * @param currency  target currency
     * @param reason    audit/log description for this transaction
     * @return future yielding the transaction response
     */
    @NotNull
    CompletableFuture<EconomyResponse> deposit(@NotNull UUID accountId,
                                              @NotNull BigDecimal amount,
                                              @NotNull Currency currency,
                                              @Nullable String reason);

    /**
     * Deposits funds into the given account using default currency.
     */
    @NotNull
    default CompletableFuture<EconomyResponse> deposit(@NotNull UUID accountId,
                                                      @NotNull BigDecimal amount,
                                                      @Nullable String reason) {
        return deposit(accountId, amount, getDefaultCurrency(), reason);
    }

    /**
     * Withdraws funds from the given account.
     *
     * @param accountId target account UUID
     * @param amount    positive amount to withdraw
     * @param currency  target currency
     * @param reason    audit/log description for this transaction
     * @return future yielding the transaction response
     */
    @NotNull
    CompletableFuture<EconomyResponse> withdraw(@NotNull UUID accountId,
                                               @NotNull BigDecimal amount,
                                               @NotNull Currency currency,
                                               @Nullable String reason);

    /**
     * Withdraws funds from the given account using default currency.
     */
    @NotNull
    default CompletableFuture<EconomyResponse> withdraw(@NotNull UUID accountId,
                                                       @NotNull BigDecimal amount,
                                                       @Nullable String reason) {
        return withdraw(accountId, amount, getDefaultCurrency(), reason);
    }

    /**
     * Transfers funds atomically between two accounts.
     *
     * @param fromAccount source account UUID
     * @param toAccount   destination account UUID
     * @param amount      positive amount to transfer
     * @param currency    target currency
     * @param reason      audit/log description
     * @return future yielding the transfer response
     */
    @NotNull
    CompletableFuture<EconomyResponse> transfer(@NotNull UUID fromAccount,
                                               @NotNull UUID toAccount,
                                               @NotNull BigDecimal amount,
                                               @NotNull Currency currency,
                                               @Nullable String reason);

    /**
     * Transfers funds atomically between two accounts using default currency.
     */
    @NotNull
    default CompletableFuture<EconomyResponse> transfer(@NotNull UUID fromAccount,
                                                       @NotNull UUID toAccount,
                                                       @NotNull BigDecimal amount,
                                                       @Nullable String reason) {
        return transfer(fromAccount, toAccount, amount, getDefaultCurrency(), reason);
    }

    /**
     * Formats an amount using the specified currency's conventions.
     *
     * @param amount   numeric amount
     * @param currency currency descriptor
     * @return formatted string
     */
    @NotNull
    default String format(@NotNull BigDecimal amount, @NotNull Currency currency) {
        return currency.format(amount);
    }

    /**
     * Formats an amount using the default currency's conventions.
     *
     * @param amount numeric amount
     * @return formatted string
     */
    @NotNull
    default String format(@NotNull BigDecimal amount) {
        return format(amount, getDefaultCurrency());
    }
}
