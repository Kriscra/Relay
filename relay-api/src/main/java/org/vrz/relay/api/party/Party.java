package org.vrz.relay.api.party;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Represents an immutable snapshot or state of a party / team within the server.
 */
public interface Party {

    /**
     * Gets the unique identifier of this party.
     *
     * @return party unique ID
     */
    @NotNull
    UUID getId();

    /**
     * Gets the display name of this party.
     *
     * @return party name
     */
    @NotNull
    String getName();

    /**
     * Gets the UUID of the party leader.
     *
     * @return leader UUID
     */
    @NotNull
    UUID getLeaderId();

    /**
     * Gets an unmodifiable set of all member UUIDs, including the leader.
     *
     * @return set of member UUIDs
     */
    @NotNull
    Set<UUID> getMembers();

    /**
     * Gets the current number of players in this party.
     *
     * @return member count
     */
    default int getSize() {
        return getMembers().size();
    }

    /**
     * Gets the maximum member capacity for this party.
     *
     * @return maximum members, or -1 if unlimited
     */
    int getMaxSize();

    /**
     * Checks whether friendly fire (PVP damage between party members) is enabled.
     *
     * @return true if party members can damage each other, false if protected
     */
    boolean isFriendlyFireEnabled();

    /**
     * Gets the timestamp when this party was formed.
     *
     * @return creation timestamp
     */
    @NotNull
    Instant getCreatedAt();

    /**
     * Checks if the specified player is the leader of this party.
     *
     * @param playerId player UUID
     * @return true if player is the leader
     */
    default boolean isLeader(@NotNull UUID playerId) {
        return getLeaderId().equals(playerId);
    }

    /**
     * Checks if the specified player is currently a member of this party.
     *
     * @param playerId player UUID
     * @return true if player is a member
     */
    default boolean isMember(@NotNull UUID playerId) {
        return getMembers().contains(playerId);
    }

    /**
     * Gets custom arbitrary metadata stored on this party.
     *
     * @return unmodifiable metadata map
     */
    @NotNull
    default Map<String, Object> getMetadata() {
        return Collections.emptyMap();
    }

    /**
     * Gets a typed metadata value if present.
     *
     * @param key metadata key
     * @param <T> value type
     * @return optional containing metadata value
     */
    @SuppressWarnings("unchecked")
    default <T> Optional<T> getMetadata(@NotNull String key) {
        Object val = getMetadata().get(key);
        if (val == null) {
            return Optional.empty();
        }
        try {
            return Optional.of((T) val);
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }
}
