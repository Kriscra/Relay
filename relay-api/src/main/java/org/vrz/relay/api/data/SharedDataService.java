package org.vrz.relay.api.data;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.messenger.Subscription;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Universal, thread-safe shared metadata and state storage for players and global server systems.
 * <p>
 * Replaces Bukkit's legacy non-thread-safe {@code Metadatable} and cumbersome NBT PDC
 * with fast in-memory type-safe keys, TTL (Time-To-Live auto-expiration), and change observation.
 *
 * <pre>{@code
 * // Defining type-safe keys:
 * DataKey<Boolean> COMBAT_TAG = DataKey.of("combat", "tagged", Boolean.class, false);
 * DataKey<String> CLAN_ROLE = DataKey.of("clan", "role", String.class);
 *
 * // Setting a temporary 15-second combat tag:
 * sharedData.setTemporary(player.getUniqueId(), COMBAT_TAG, true, Duration.ofSeconds(15), myPlugin);
 *
 * // Checking state from another plugin (e.g. Teleport plugin):
 * if (sharedData.getOrKeyDefault(player.getUniqueId(), COMBAT_TAG)) {
 *     player.sendMessage("Savaş halindeyken ışınlanamazsınız!");
 *     return;
 * }
 * }</pre>
 */
public interface SharedDataService {

    /**
     * Stores a value under a type-safe data key for the given player.
     *
     * @param targetId player UUID
     * @param key      type-safe key
     * @param value    value to store
     * @param plugin   owning plugin (can be null)
     * @param <T>      value type
     */
    <T> void set(@NotNull UUID targetId,
                 @NotNull DataKey<T> key,
                 @NotNull T value,
                 @Nullable Plugin plugin);

    /**
     * Stores a value under a type-safe data key.
     */
    default <T> void set(@NotNull UUID targetId, @NotNull DataKey<T> key, @NotNull T value) {
        set(targetId, key, value, null);
    }

    /**
     * Stores a temporary value that automatically expires and is purged after the given TTL.
     *
     * @param targetId player UUID
     * @param key      type-safe key
     * @param value    value to store
     * @param ttl      duration before this entry is automatically removed
     * @param plugin   owning plugin
     * @param <T>      value type
     */
    <T> void setTemporary(@NotNull UUID targetId,
                          @NotNull DataKey<T> key,
                          @NotNull T value,
                          @NotNull Duration ttl,
                          @Nullable Plugin plugin);

    /**
     * Stores a temporary value that automatically expires after the given TTL.
     */
    default <T> void setTemporary(@NotNull UUID targetId,
                                  @NotNull DataKey<T> key,
                                  @NotNull T value,
                                  @NotNull Duration ttl) {
        setTemporary(targetId, key, value, ttl, null);
    }

    /**
     * Retrieves the stored value for the given player and key.
     *
     * @param targetId player UUID
     * @param key      type-safe key
     * @param <T>      value type
     * @return optional containing the value, or empty if not present or expired
     */
    @NotNull
    <T> Optional<T> get(@NotNull UUID targetId, @NotNull DataKey<T> key);

    /**
     * Retrieves the value, or returns the specified fallback if not present.
     */
    @NotNull
    default <T> T getOrDefault(@NotNull UUID targetId, @NotNull DataKey<T> key, @NotNull T fallback) {
        return get(targetId, key).orElse(fallback);
    }

    /**
     * Retrieves the value, or returns the key's defined default value (or throws if no default).
     */
    @NotNull
    default <T> T getOrKeyDefault(@NotNull UUID targetId, @NotNull DataKey<T> key) {
        return get(targetId, key)
                .or(key::getDefaultValue)
                .orElseThrow(() -> new IllegalStateException("No value or default found for key: " + key.getFullKey()));
    }

    /**
     * Checks if a non-expired value exists for this player and key.
     */
    boolean has(@NotNull UUID targetId, @NotNull DataKey<?> key);

    /**
     * Removes and returns the value stored under the given key.
     *
     * @param targetId player UUID
     * @param key      type-safe key
     * @param <T>      value type
     * @return optional containing the removed value
     */
    @NotNull
    <T> Optional<T> remove(@NotNull UUID targetId, @NotNull DataKey<T> key);

    /**
     * Clears all shared data stored for the given player.
     *
     * @param targetId player UUID
     */
    void clearAll(@NotNull UUID targetId);

    /**
     * Gets a snapshot map of all currently active data keys and values for this player.
     */
    @NotNull
    Map<DataKey<?>, Object> getAll(@NotNull UUID targetId);

    /**
     * Stores a global server-wide shared value.
     */
    <T> void setGlobal(@NotNull DataKey<T> key, @NotNull T value, @Nullable Plugin plugin);

    default <T> void setGlobal(@NotNull DataKey<T> key, @NotNull T value) {
        setGlobal(key, value, null);
    }

    /**
     * Stores a global temporary value with TTL.
     */
    <T> void setGlobalTemporary(@NotNull DataKey<T> key,
                                @NotNull T value,
                                @NotNull Duration ttl,
                                @Nullable Plugin plugin);

    default <T> void setGlobalTemporary(@NotNull DataKey<T> key, @NotNull T value, @NotNull Duration ttl) {
        setGlobalTemporary(key, value, ttl, null);
    }

    /**
     * Retrieves a global server-wide value.
     */
    @NotNull
    <T> Optional<T> getGlobal(@NotNull DataKey<T> key);

    /**
     * Checks if a global value is present and non-expired.
     */
    boolean hasGlobal(@NotNull DataKey<?> key);

    /**
     * Removes a global value.
     */
    @NotNull
    <T> Optional<T> removeGlobal(@NotNull DataKey<T> key);

    /**
     * Subscribes a listener to observe changes on the specified data key across all targets.
     *
     * @param key      key to observe
     * @param observer owning plugin
     * @param listener change callback
     * @param <T>      value type
     * @return Subscription handle to unregister
     */
    @NotNull
    <T> Subscription observe(@NotNull DataKey<T> key,
                            @NotNull Plugin observer,
                            @NotNull DataChangeListener<T> listener);

    /**
     * Cleans up all entries registered by the specified plugin (called on plugin disable).
     *
     * @param plugin owning plugin
     */
    void clearAll(@NotNull Plugin plugin);
}
