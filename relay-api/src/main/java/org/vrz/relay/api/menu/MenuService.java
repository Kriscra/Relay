package org.vrz.relay.api.menu;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

/**
 * Central service for creating, managing, and tracking Relay virtual chest menus.
 * Designed to be zero-leak and multi-thread safe across Paper and Folia architectures.
 */
public interface MenuService {

    /**
     * Creates a new fluent {@link MenuBuilder} owned by the given plugin.
     *
     * @param plugin owning plugin
     * @return new MenuBuilder
     */
    @NotNull
    MenuBuilder createBuilder(@NotNull Plugin plugin);

    /**
     * Creates a new paginated menu builder for displaying collections of items.
     *
     * @param plugin owning plugin
     * @param <T>    item data model type
     * @return new PaginatedMenuBuilder
     */
    @NotNull
    <T> PaginatedMenuBuilder<T> createPaginatedBuilder(@NotNull Plugin plugin);

    /**
     * Creates a new fluent {@link MenuButtonBuilder}.
     *
     * @return new MenuButtonBuilder
     */
    @NotNull
    MenuButtonBuilder createButtonBuilder();

    /**
     * Gets the active Relay virtual menu currently opened by the player, if any.
     *
     * @param player player
     * @return optional menu
     */
    @NotNull
    Optional<Menu> getOpenMenu(@NotNull Player player);

    /**
     * Closes the active virtual menu for the given player if one is open.
     *
     * @param player player
     */
    void closeOpenMenu(@NotNull Player player);

    /**
     * Gets all currently active menu instances across the server.
     *
     * @return set of active menus
     */
    @NotNull
    Set<Menu> getActiveMenus();

    /**
     * Closes all active Relay menus for all players.
     */
    void closeAll();

    /**
     * Closes and unregisters all menus owned by the given plugin.
     * Called automatically when an external plugin disables to prevent leaks.
     *
     * @param plugin owning plugin
     */
    void closeAll(@NotNull Plugin plugin);
}
