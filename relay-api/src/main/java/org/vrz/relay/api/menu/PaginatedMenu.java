package org.vrz.relay.api.menu;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Extension of {@link Menu} providing built-in pagination over a list of items or data models.
 *
 * @param <T> data model type
 */
public interface PaginatedMenu<T> extends Menu {

    /**
     * Gets the 0-indexed page currently viewed by the player.
     *
     * @param player player
     * @return current page index
     */
    int getCurrentPage(@NotNull Player player);

    /**
     * Gets the total number of pages available.
     *
     * @return max page count (at least 1)
     */
    int getMaxPages();

    /**
     * Changes the current page for the given player.
     *
     * @param player player
     * @param page   target page index
     */
    void setPage(@NotNull Player player, int page);

    /**
     * Advances to the next page if available.
     *
     * @param player player
     */
    void nextPage(@NotNull Player player);

    /**
     * Navigates back to the previous page if available.
     *
     * @param player player
     */
    void previousPage(@NotNull Player player);

    /**
     * Gets the underlying list of items being paginated.
     *
     * @return list of items
     */
    @NotNull
    List<T> getItems();

    /**
     * Replaces the list of paginated items and updates all active viewer pages.
     *
     * @param items new items
     */
    void setItems(@NotNull List<T> items);

    /**
     * Appends an item to the paginated list.
     *
     * @param item item to add
     */
    void addItem(@NotNull T item);

    /**
     * Removes an item from the paginated list.
     *
     * @param item item to remove
     */
    void removeItem(@NotNull T item);
}
