package org.vrz.relay.api.scoreboard;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

/**
 * Enterprise-grade, flicker-free Scoreboard and Tablist engine.
 * <p>
 * Manages per-player isolated virtual sidebars, dynamic 1-15 line text,
 * animated titles, auto-refresh loops, and tablist header/footers.
 * Fully multi-threaded and Folia region-safe.
 */
public interface ScoreboardService {

    /**
     * Creates a new fluent {@link SidebarBuilder} owned by the given plugin.
     *
     * @param plugin owning plugin
     * @return new sidebar builder
     */
    @NotNull
    SidebarBuilder sidebarBuilder(@NotNull Plugin plugin);

    /**
     * Gets the active {@link Sidebar} currently shown to the player, if any.
     *
     * @param player target player
     * @return optional containing the active sidebar
     */
    @NotNull
    Optional<Sidebar> getSidebar(@NotNull Player player);

    /**
     * Removes and destroys the active sidebar for the player,
     * restoring their default main scoreboard.
     *
     * @param player target player
     */
    void removeSidebar(@NotNull Player player);

    /**
     * Updates or sets the player's tablist header and footer components.
     *
     * @param player target player
     * @param header header component, or {@code null} to clear
     * @param footer footer component, or {@code null} to clear
     */
    void setTablist(@NotNull Player player, @Nullable Component header, @Nullable Component footer);

    /**
     * Updates or sets the player's tablist header and footer using MiniMessage format strings.
     *
     * @param player target player
     * @param header MiniMessage header, or {@code null} to clear
     * @param footer MiniMessage footer, or {@code null} to clear
     */
    void setTablistMiniMessage(@NotNull Player player, @Nullable String header, @Nullable String footer);

    /**
     * Resets the player's tablist header and footer back to empty defaults.
     *
     * @param player target player
     */
    void resetTablist(@NotNull Player player);

    /**
     * Zero-leak lifecycle cleanup: Removes and destroys all sidebars and tablists
     * created by the specified plugin.
     *
     * @param plugin plugin being disabled
     */
    void clearAll(@NotNull Plugin plugin);

    /**
     * Server shutdown cleanup: Clears all active sidebars and resets all players' scoreboards.
     */
    void clearAll();

    /**
     * Gets the total number of currently active sidebars managed by Relay.
     *
     * @return count of active sidebars
     */
    int getActiveSidebarCount();

    /**
     * Gets all currently active sidebars across all players.
     *
     * @return collection of active sidebars
     */
    @NotNull
    Collection<Sidebar> getActiveSidebars();
}
