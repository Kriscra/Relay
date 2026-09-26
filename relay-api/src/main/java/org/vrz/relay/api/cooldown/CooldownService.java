package org.vrz.relay.api.cooldown;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Universal, thread-safe cooldown manager for players and global server systems.
 * <p>
 * Eliminates custom memory-leaking {@code HashMap<UUID, Long>} structures across plugins,
 * provides automatic background eviction, progress calculations, and expiration events.
 *
 * <pre>{@code
 * // Applying a 15-second cooldown to a player
 * cooldownService.setCooldown(player.getUniqueId(), "kit:starter", Duration.ofSeconds(15), myPlugin);
 *
 * // Checking remaining time
 * if (cooldownService.hasCooldown(player.getUniqueId(), "kit:starter")) {
 *     String remaining = cooldownService.formatRemaining(player.getUniqueId(), "kit:starter");
 *     player.sendMessage("Lütfen bekleyin: " + remaining);
 *     return;
 * }
 * }</pre>
 */
public interface CooldownService {

    /**
     * Applies a cooldown to a specific player UUID with an optional plugin tag and expiration callback.
     *
     * @param targetId player UUID
     * @param key      unique cooldown key (e.g. "ability:dash" or "command:home")
     * @param duration cooldown length
     * @param plugin   owning plugin for automatic cleanup on disable (can be null)
     * @param onExpire callback executed when this specific cooldown reaches completion
     * @return created CooldownEntry snapshot
     */
    @NotNull
    CooldownEntry setCooldown(@NotNull UUID targetId,
                              @NotNull String key,
                              @NotNull Duration duration,
                              @Nullable Plugin plugin,
                              @Nullable Consumer<CooldownEntry> onExpire);

    /**
     * Applies a cooldown with an owning plugin.
     */
    @NotNull
    default CooldownEntry setCooldown(@NotNull UUID targetId,
                                      @NotNull String key,
                                      @NotNull Duration duration,
                                      @Nullable Plugin plugin) {
        return setCooldown(targetId, key, duration, plugin, null);
    }

    /**
     * Applies a cooldown without specifying an owning plugin.
     */
    @NotNull
    default CooldownEntry setCooldown(@NotNull UUID targetId,
                                      @NotNull String key,
                                      @NotNull Duration duration) {
        return setCooldown(targetId, key, duration, null, null);
    }

    /**
     * Checks if the player currently has an active (non-expired) cooldown for this key.
     *
     * @param targetId player UUID
     * @param key      cooldown identifier
     * @return true if still on cooldown
     */
    boolean hasCooldown(@NotNull UUID targetId, @NotNull String key);

    /**
     * Retrieves the remaining duration for this player and cooldown key.
     *
     * @param targetId player UUID
     * @param key      cooldown identifier
     * @return remaining Duration, or {@link Duration#ZERO} if not on cooldown
     */
    @NotNull
    Duration getRemaining(@NotNull UUID targetId, @NotNull String key);

    /**
     * Retrieves the remaining milliseconds.
     */
    default long getRemainingMillis(@NotNull UUID targetId, @NotNull String key) {
        return getRemaining(targetId, key).toMillis();
    }

    /**
     * Formats the remaining time into a compact human-readable string (e.g. "2m 30s", "4.2s").
     *
     * @param targetId player UUID
     * @param key      cooldown identifier
     * @return formatted compact time string
     */
    @NotNull
    default String formatRemaining(@NotNull UUID targetId, @NotNull String key) {
        return CooldownFormatter.formatCompact(getRemaining(targetId, key));
    }

    /**
     * Gets the full {@link CooldownEntry} descriptor for an active cooldown.
     *
     * @param targetId player UUID
     * @param key      cooldown identifier
     * @return optional containing the entry if active
     */
    @NotNull
    Optional<CooldownEntry> getCooldown(@NotNull UUID targetId, @NotNull String key);

    /**
     * Manually clears and removes a cooldown for a player.
     *
     * @param targetId player UUID
     * @param key      cooldown identifier
     * @return true if a cooldown was removed
     */
    boolean clearCooldown(@NotNull UUID targetId, @NotNull String key);

    /**
     * Clears all active cooldowns for a given player UUID.
     *
     * @param targetId player UUID
     */
    void clearAllCooldowns(@NotNull UUID targetId);

    /**
     * Gets a map of all active cooldown entries for the given player.
     *
     * @param targetId player UUID
     * @return unmodifiable map of key to entry
     */
    @NotNull
    Map<String, CooldownEntry> getActiveCooldowns(@NotNull UUID targetId);

    /**
     * Applies a global server-wide cooldown (not bound to any player).
     *
     * @param key      cooldown identifier
     * @param duration cooldown duration
     * @param plugin   owning plugin
     * @return created CooldownEntry snapshot
     */
    @NotNull
    CooldownEntry setGlobalCooldown(@NotNull String key,
                                    @NotNull Duration duration,
                                    @Nullable Plugin plugin);

    /**
     * Applies a global server-wide cooldown.
     */
    @NotNull
    default CooldownEntry setGlobalCooldown(@NotNull String key, @NotNull Duration duration) {
        return setGlobalCooldown(key, duration, null);
    }

    /**
     * Checks if a global server cooldown is currently active.
     */
    boolean hasGlobalCooldown(@NotNull String key);

    /**
     * Retrieves the remaining duration for a global server cooldown.
     */
    @NotNull
    Duration getGlobalRemaining(@NotNull String key);

    /**
     * Manually clears a global server cooldown.
     */
    boolean clearGlobalCooldown(@NotNull String key);

    /**
     * Clears all cooldowns registered by the given plugin (called on plugin disable).
     *
     * @param plugin owning plugin
     */
    void clearAllCooldowns(@NotNull Plugin plugin);
}
