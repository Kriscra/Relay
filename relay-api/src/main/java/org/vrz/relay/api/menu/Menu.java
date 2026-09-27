package org.vrz.relay.api.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a virtual chest or inventory menu managed by Relay.
 * Handles slot binding, player interactions, dynamic updates, and viewer sessions.
 */
public interface Menu {

    /**
     * Unique identifier for this menu instance.
     *
     * @return menu UUID
     */
    @NotNull
    UUID getId();

    /**
     * Gets the plugin that owns and manages this menu.
     *
     * @return owning plugin
     */
    @NotNull
    Plugin getPlugin();

    /**
     * Gets the title component of this menu.
     *
     * @return menu title
     */
    @NotNull
    Component getTitle();

    /**
     * Gets the number of rows in this chest menu (1 to 6).
     *
     * @return row count
     */
    int getRows();

    /**
     * Gets the total slot size (rows * 9).
     *
     * @return total slots
     */
    int getSize();

    /**
     * Gets the button placed at the specified slot index, if present.
     *
     * @param slot slot index (0 to size - 1)
     * @return optional button
     */
    @NotNull
    Optional<MenuButton> getButton(int slot);

    /**
     * Sets or replaces the button at the specified slot index.
     *
     * @param slot   slot index
     * @param button button to set, or null to clear
     */
    void setButton(int slot, @Nullable MenuButton button);

    /**
     * Sets or replaces the button using 1-based (row, col) grid coordinates.
     *
     * @param row    row number (1 to rows)
     * @param col    column number (1 to 9)
     * @param button button to set, or null to clear
     */
    void setButton(int row, int col, @Nullable MenuButton button);

    /**
     * Removes the button at the specified slot index.
     *
     * @param slot slot index
     */
    void removeButton(int slot);

    /**
     * Fills all perimeter border slots with the given button.
     *
     * @param button border button
     */
    void fillBorder(@NotNull MenuButton button);

    /**
     * Fills all empty slots with the given background button.
     *
     * @param button background filler button
     */
    void fillBackground(@NotNull MenuButton button);

    /**
     * Fills a rectangular region bounded by (fromRow, fromCol) to (toRow, toCol).
     *
     * @param fromRow top row (1 to rows)
     * @param fromCol left column (1 to 9)
     * @param toRow   bottom row (1 to rows)
     * @param toCol   right column (1 to 9)
     * @param button  button to place in the rectangle
     */
    void fillRectangle(int fromRow, int fromCol, int toRow, int toCol, @NotNull MenuButton button);

    /**
     * Updates and re-renders a single slot for an actively viewing player.
     *
     * @param player viewing player
     * @param slot   slot index
     */
    void updateSlot(@NotNull Player player, int slot);

    /**
     * Updates and re-renders all slots for an actively viewing player.
     *
     * @param player viewing player
     */
    void updateAllSlots(@NotNull Player player);

    /**
     * Opens this menu for the given player.
     *
     * @param player player to open for
     */
    void open(@NotNull Player player);

    /**
     * Closes this menu for the specified player.
     *
     * @param player player
     */
    void close(@NotNull Player player);

    /**
     * Gets an unmodifiable set of all players currently viewing this menu.
     *
     * @return set of viewing player UUIDs
     */
    @NotNull
    Set<UUID> getViewers();

    /**
     * Checks if the given player is currently viewing this menu.
     *
     * @param player player
     * @return true if viewing
     */
    boolean isViewer(@NotNull Player player);

    /**
     * Refreshes all contents for all active viewers.
     */
    void refreshViewers();

    /**
     * Closes this menu for all currently viewing players.
     */
    void closeAll();

    /**
     * Gets the raw inventory currently rendered for the given player, if open.
     *
     * @param player player
     * @return optional inventory
     */
    @NotNull
    Optional<Inventory> getInventory(@NotNull Player player);
}
