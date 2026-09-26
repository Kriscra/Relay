package org.vrz.relay.api.party;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Modern, decoupled service providing party, team, and friendly-fire management.
 * <p>
 * Offers non-blocking asynchronous queries as well as high-performance,
 * zero-allocation synchronous friendly-fire checks designed for combat and damage events.
 */
public interface PartyService {

    /**
     * Retrieves the party that the specified player belongs to, if any.
     *
     * @param playerId player unique ID
     * @return future containing optional party
     */
    @NotNull
    CompletableFuture<Optional<Party>> getParty(@NotNull UUID playerId);

    /**
     * Retrieves a party by its unique ID.
     *
     * @param partyId party unique ID
     * @return future containing optional party
     */
    @NotNull
    CompletableFuture<Optional<Party>> getPartyById(@NotNull UUID partyId);

    /**
     * Checks if the specified player is currently in any party.
     *
     * @param playerId player unique ID
     * @return future containing true if player is in a party
     */
    @NotNull
    CompletableFuture<Boolean> inParty(@NotNull UUID playerId);

    /**
     * Checks if two players belong to the exact same party.
     *
     * @param playerA first player unique ID
     * @param playerB second player unique ID
     * @return future containing true if both players are teammates in the same party
     */
    @NotNull
    CompletableFuture<Boolean> inSameParty(@NotNull UUID playerA, @NotNull UUID playerB);

    /**
     * Checks if the specified player is the leader of their party.
     *
     * @param playerId player unique ID
     * @return future containing true if player is party leader
     */
    @NotNull
    CompletableFuture<Boolean> isLeader(@NotNull UUID playerId);

    /**
     * Retrieves the set of all member UUIDs in the player's party.
     * If the player is not in a party, returns an empty set.
     *
     * @param playerId player unique ID
     * @return future containing set of member UUIDs
     */
    @NotNull
    CompletableFuture<Set<UUID>> getPartyMembers(@NotNull UUID playerId);

    /**
     * Retrieves all currently active parties on the server.
     *
     * @return future containing collection of active parties
     */
    @NotNull
    CompletableFuture<Collection<Party>> getActiveParties();

    /**
     * Fast, lock-free synchronous friendly-fire check.
     * <p>
     * Designed specifically for high-frequency combat events (e.g. {@code EntityDamageByEntityEvent},
     * projectile impacts, custom spell/skill casts) without thread blocking.
     *
     * @param damager UUID of the attacking entity/player
     * @param victim  UUID of the victim player
     * @return {@code true} if both players are in the same party AND friendly fire is disabled
     *         (meaning damage should be cancelled/protected).
     *         {@code false} if they are not in the same party, or if friendly fire is allowed.
     */
    boolean isFriendlySync(@NotNull UUID damager, @NotNull UUID victim);

    /**
     * Asynchronous friendly-fire check.
     *
     * @param damager UUID of the attacking entity/player
     * @param victim  UUID of the victim player
     * @return future containing true if damage should be prevented
     */
    @NotNull
    CompletableFuture<Boolean> isFriendly(@NotNull UUID damager, @NotNull UUID victim);

    /**
     * Convenience method to check whether an attack/damage is permitted.
     * Equivalent to {@code !isFriendlySync(damager, victim)}.
     *
     * @param damager UUID of the attacking entity/player
     * @param victim  UUID of the victim player
     * @return true if damage is allowed, false if damage should be blocked
     */
    default boolean canDamageSync(@NotNull UUID damager, @NotNull UUID victim) {
        return !isFriendlySync(damager, victim);
    }

    /**
     * Convenience asynchronous method to check whether attack/damage is permitted.
     *
     * @param damager UUID of the attacking entity/player
     * @param victim  UUID of the victim player
     * @return future containing true if damage is allowed
     */
    @NotNull
    default CompletableFuture<Boolean> canDamage(@NotNull UUID damager, @NotNull UUID victim) {
        return isFriendly(damager, victim).thenApply(friendly -> !friendly);
    }

    /**
     * Creates a new party with default capacity (8 members).
     *
     * @param leaderId unique ID of the party founder/leader
     * @param name     party display name
     * @return future containing the newly created party
     */
    @NotNull
    default CompletableFuture<Party> createParty(@NotNull UUID leaderId, @NotNull String name) {
        return createParty(leaderId, name, 8);
    }

    /**
     * Creates a new party with a custom maximum capacity.
     *
     * @param leaderId unique ID of the party founder/leader
     * @param name     party display name
     * @param maxSize  maximum members allowed, or -1 for unlimited
     * @return future containing the newly created party
     */
    @NotNull
    CompletableFuture<Party> createParty(@NotNull UUID leaderId, @NotNull String name, int maxSize);

    /**
     * Disbands an active party.
     *
     * @param partyId unique ID of the party to disband
     * @return future containing true if party was successfully disbanded
     */
    @NotNull
    CompletableFuture<Boolean> disbandParty(@NotNull UUID partyId);

    /**
     * Adds a new member to an existing party.
     *
     * @param partyId  party unique ID
     * @param memberId new member player UUID
     * @return future containing true if member was added, false if full, not found, or already in a party
     */
    @NotNull
    CompletableFuture<Boolean> addMember(@NotNull UUID partyId, @NotNull UUID memberId);

    /**
     * Removes a member from an existing party.
     * If the leader leaves and other members exist, leadership is transferred.
     * If the last member leaves, the party is automatically disbanded.
     *
     * @param partyId  party unique ID
     * @param memberId member player UUID to remove
     * @return future containing true if member was removed
     */
    @NotNull
    CompletableFuture<Boolean> removeMember(@NotNull UUID partyId, @NotNull UUID memberId);

    /**
     * Transfers party leadership to another existing member.
     *
     * @param partyId     party unique ID
     * @param newLeaderId UUID of existing member to promote to leader
     * @return future containing true if leadership was transferred
     */
    @NotNull
    CompletableFuture<Boolean> setLeader(@NotNull UUID partyId, @NotNull UUID newLeaderId);

    /**
     * Sets whether friendly fire is enabled for this party.
     *
     * @param partyId party unique ID
     * @param enabled true to allow members to hurt each other, false to protect
     * @return future containing true if setting was updated
     */
    @NotNull
    CompletableFuture<Boolean> setFriendlyFire(@NotNull UUID partyId, boolean enabled);
}
