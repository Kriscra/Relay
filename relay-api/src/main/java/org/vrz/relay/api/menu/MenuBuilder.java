package org.vrz.relay.api.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Fluent builder for creating declarative {@link Menu} instances.
 */
public interface MenuBuilder {

    /**
     * Sets the title of the menu as an Adventure Component.
     *
     * @param title title component
     * @return this builder
     */
    @NotNull
    MenuBuilder title(@NotNull Component title);

    /**
     * Sets the title of the menu parsed via MiniMessage.
     *
     * @param miniMessageTitle MiniMessage string
     * @return this builder
     */
    @NotNull
    MenuBuilder titleMiniMessage(@NotNull String miniMessageTitle);

    /**
     * Sets the number of chest rows (1 to 6).
     *
     * @param rows row count
     * @return this builder
     */
    @NotNull
    MenuBuilder rows(int rows);

    /**
     * Places a button at the specified slot index.
     *
     * @param slot   slot index (0 to size - 1)
     * @param button the button
     * @return this builder
     */
    @NotNull
    MenuBuilder button(int slot, @NotNull MenuButton button);

    /**
     * Convenience method to place an item with a click handler at the given slot.
     *
     * @param slot    slot index
     * @param item    item stack
     * @param onClick click action
     * @return this builder
     */
    @NotNull
    MenuBuilder button(int slot, @NotNull ItemStack item, @NotNull Consumer<MenuClickContext> onClick);

    /**
     * Places a button using 1-based (row, col) grid coordinates.
     *
     * @param row    row number (1 to rows)
     * @param col    column number (1 to 9)
     * @param button the button
     * @return this builder
     */
    @NotNull
    MenuBuilder button(int row, int col, @NotNull MenuButton button);

    /**
     * Applies a row-by-row character pattern mask to define layout.
     * Each row in pattern corresponds to an inventory row (must have length 9).
     * Keys mapped via {@link #bindKey(char, MenuButton)} will fill the slots.
     *
     * @param pattern array of strings
     * @return this builder
     */
    @NotNull
    MenuBuilder pattern(@NotNull String... pattern);

    /**
     * Binds a character key used in {@link #pattern(String...)} to a specific button.
     *
     * @param key    character key
     * @param button button to place where key appears
     * @return this builder
     */
    @NotNull
    MenuBuilder bindKey(char key, @NotNull MenuButton button);

    /**
     * Automatically fills the perimeter border slots with the specified button.
     *
     * @param button border button
     * @return this builder
     */
    @NotNull
    MenuBuilder fillBorder(@NotNull MenuButton button);

    /**
     * Fills any unoccupied slots with the specified background button.
     *
     * @param button background filler
     * @return this builder
     */
    @NotNull
    MenuBuilder fillBackground(@NotNull MenuButton button);

    /**
     * Callback invoked when a player opens this menu.
     *
     * @param onOpen consumer receiving player
     * @return this builder
     */
    @NotNull
    MenuBuilder onOpen(@NotNull Consumer<Player> onOpen);

    /**
     * Callback invoked when a player closes this menu.
     *
     * @param onClose consumer receiving player
     * @return this builder
     */
    @NotNull
    MenuBuilder onClose(@NotNull Consumer<Player> onClose);

    /**
     * Whether clicks inside the player's bottom inventory are allowed while viewing.
     * Defaults to false (preventing shift-click dupe exploits).
     *
     * @param allow true to allow player inventory clicks
     * @return this builder
     */
    @NotNull
    MenuBuilder allowPlayerInventoryClicks(boolean allow);

    /**
     * Enables automatic recurring refresh of all menu slots every N ticks while viewers are active.
     * Automatically paused/cancelled when there are no viewers to save CPU ticks.
     *
     * @param periodTicks interval in ticks (e.g. 20L = 1 second)
     * @return this builder
     */
    @NotNull
    MenuBuilder refreshInterval(long periodTicks);

    /**
     * Builds and registers the configured {@link Menu}.
     *
     * @return new Menu
     */
    @NotNull
    Menu build();

    /**
     * Builds the menu and immediately opens it for the given player.
     *
     * @param player player to open for
     */
    default void open(@NotNull Player player) {
        build().open(player);
    }
}
