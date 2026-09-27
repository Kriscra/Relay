package org.vrz.relay.api.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Contextual information provided when a player interacts with a menu button.
 */
public interface MenuClickContext {

    /**
     * Gets the player who clicked the menu.
     *
     * @return the player
     */
    @NotNull
    Player getPlayer();

    /**
     * Gets the type of click performed (e.g. LEFT, RIGHT, SHIFT_LEFT, NUMBER_KEY, etc.).
     *
     * @return the click type
     */
    @NotNull
    ClickType getClickType();

    /**
     * Gets the raw slot index clicked in the top inventory.
     *
     * @return slot index
     */
    int getSlot();

    /**
     * Gets the item stack located in the clicked slot, or null if empty.
     *
     * @return the clicked item
     */
    @Nullable
    ItemStack getClickedItem();

    /**
     * Gets the menu instance in which this click occurred.
     *
     * @return the menu
     */
    @NotNull
    Menu getMenu();

    /**
     * Checks if the underlying inventory click event is cancelled.
     *
     * @return true if cancelled
     */
    boolean isCancelled();

    /**
     * Sets whether the click event should be cancelled. Defaults to true in Relay menus.
     *
     * @param cancelled true to cancel
     */
    void setCancelled(boolean cancelled);

    /**
     * Safely closes the menu for the clicking player.
     */
    void close();

    /**
     * Refreshes the menu contents for the clicking player.
     */
    void refresh();

    /**
     * Opens another menu for the clicking player, transitioning smoothly.
     *
     * @param otherMenu the menu to open
     */
    void open(@NotNull Menu otherMenu);
}
