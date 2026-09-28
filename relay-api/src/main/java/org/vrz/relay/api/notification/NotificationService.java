package org.vrz.relay.api.notification;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

/**
 * Enterprise-grade notification and player feedback engine.
 * <p>
 * Manages priority-based ActionBar queues, smooth Title sequences,
 * animated countdown BossBars, and virtual Toast advancement popups.
 * Fully multi-threaded and region-safe across Paper and Folia.
 */
public interface NotificationService {

    /**
     * Creates a new fluent {@link ActionBarBuilder} for the specified player.
     *
     * @param player target player
     * @return new action bar builder
     */
    @NotNull
    ActionBarBuilder actionBar(@NotNull Player player);

    /**
     * Creates a new fluent {@link TitleBuilder} for the specified player.
     *
     * @param player target player
     * @return new title builder
     */
    @NotNull
    TitleBuilder title(@NotNull Player player);

    /**
     * Creates a new fluent {@link BossBarBuilder} for the specified player.
     *
     * @param player target player
     * @return new boss bar builder
     */
    @NotNull
    BossBarBuilder bossBar(@NotNull Player player);

    /**
     * Creates a new fluent {@link ToastBuilder} for the specified player.
     *
     * @param player target player
     * @return new toast builder
     */
    @NotNull
    ToastBuilder toast(@NotNull Player player);

    /**
     * Registers a continuous background ActionBar provider for a player (e.g. RPG Mana/HP HUD).
     * Shown whenever no higher priority notification is active in the queue.
     *
     * @param plugin   owning plugin
     * @param player   target player
     * @param provider continuous content supplier
     */
    void registerContinuousActionBar(@NotNull Plugin plugin, @NotNull Player player, @NotNull ContinuousActionBarProvider provider);

    /**
     * Unregisters any continuous ActionBar provider registered by the plugin for this player.
     *
     * @param plugin owning plugin
     * @param player target player
     */
    void unregisterContinuousActionBar(@NotNull Plugin plugin, @NotNull Player player);

    /**
     * Gets the currently active ActionBar notification for a player, if any.
     *
     * @param player target player
     * @return optional active notification
     */
    @NotNull
    Optional<ActionBarNotification> getActiveActionBar(@NotNull Player player);

    /**
     * Gets all currently active BossBars for a player.
     *
     * @param player target player
     * @return collection of active boss bars
     */
    @NotNull
    Collection<ActiveBossBar> getActiveBossBars(@NotNull Player player);

    /**
     * Clears all queued and active notifications (ActionBar, Titles, BossBars) for a player.
     *
     * @param player target player
     */
    void clearAll(@NotNull Player player);

    /**
     * Cleans up all notifications and providers owned by a specific plugin.
     *
     * @param plugin owning plugin
     */
    void clearAll(@NotNull Plugin plugin);

    /**
     * Shuts down the notification service and cleans all active displays across the server.
     */
    void shutdown();
}
