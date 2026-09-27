package org.vrz.relay.api.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Fluent builder for creating {@link PaginatedMenu} instances.
 *
 * @param <T> data item type
 */
public interface PaginatedMenuBuilder<T> {

    /**
     * Sets the title of the menu.
     *
     * @param title title component
     * @return this builder
     */
    @NotNull
    PaginatedMenuBuilder<T> title(@NotNull Component title);

    /**
     * Sets the title parsed via MiniMessage.
     *
     * @param miniMessageTitle MiniMessage string
     * @return this builder
     */
    @NotNull
    PaginatedMenuBuilder<T> titleMiniMessage(@NotNull String miniMessageTitle);

    /**
     * Sets the number of chest rows (1 to 6).
     *
     * @param rows row count
     * @return this builder
     */
    @NotNull
    PaginatedMenuBuilder<T> rows(int rows);

    /**
     * Supplies the collection of items to be displayed across pages.
     *
     * @param items collection of items
     * @return this builder
     */
    @NotNull
    PaginatedMenuBuilder<T> items(@NotNull Collection<T> items);

    /**
     * Defines the explicit slot indices designated for paginated content.
     *
     * @param slots array of slot indices
     * @return this builder
     */
    @NotNull
    PaginatedMenuBuilder<T> contentSlots(int... slots);

    /**
     * Defines a range of slots [startSlot, endSlot] inclusive for paginated content.
     *
     * @param startSlot start slot
     * @param endSlot   end slot
     * @return this builder
     */
    @NotNull
    PaginatedMenuBuilder<T> contentSlotRange(int startSlot, int endSlot);

    /**
     * Renderer function mapping each data item and its index to a clickable or displayable {@link MenuButton}.
     *
     * @param renderer function taking (item, index) and returning MenuButton
     * @return this builder
     */
    @NotNull
    PaginatedMenuBuilder<T> itemRenderer(@NotNull BiFunction<T, Integer, MenuButton> renderer);

    /**
     * Configures a static button at a non-paginated slot.
     *
     * @param slot   slot index
     * @param button the button
     * @return this builder
     */
    @NotNull
    PaginatedMenuBuilder<T> staticButton(int slot, @NotNull MenuButton button);

    /**
     * Configures the button used to navigate to the previous page.
     *
     * @param slot           slot index
     * @param buttonSupplier function supplying button given the paginated menu
     * @return this builder
     */
    @NotNull
    PaginatedMenuBuilder<T> previousPageButton(int slot, @NotNull Function<PaginatedMenu<T>, MenuButton> buttonSupplier);

    /**
     * Configures the button used to navigate to the next page.
     *
     * @param slot           slot index
     * @param buttonSupplier function supplying button given the paginated menu
     * @return this builder
     */
    @NotNull
    PaginatedMenuBuilder<T> nextPageButton(int slot, @NotNull Function<PaginatedMenu<T>, MenuButton> buttonSupplier);

    /**
     * Configures an indicator button showing current and max pages.
     *
     * @param slot           slot index
     * @param buttonSupplier function taking (currentPage, maxPages) and returning MenuButton
     * @return this builder
     */
    @NotNull
    PaginatedMenuBuilder<T> pageInfoButton(int slot, @NotNull BiFunction<Integer, Integer, MenuButton> buttonSupplier);

    /**
     * Fills the perimeter border slots with a button.
     *
     * @param button border button
     * @return this builder
     */
    @NotNull
    PaginatedMenuBuilder<T> fillBorder(@NotNull MenuButton button);

    /**
     * Fills remaining unoccupied slots with background button.
     *
     * @param button background button
     * @return this builder
     */
    @NotNull
    PaginatedMenuBuilder<T> fillBackground(@NotNull MenuButton button);

    /**
     * Builds the {@link PaginatedMenu}.
     *
     * @return paginated menu
     */
    @NotNull
    PaginatedMenu<T> build();

    /**
     * Builds and opens for the player immediately.
     *
     * @param player player
     */
    default void open(@NotNull Player player) {
        build().open(player);
    }
}
