package org.vrz.relay.api.menu;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Fluent builder for constructing {@link MenuButton} instances.
 */
public interface MenuButtonBuilder {

    /**
     * Sets a static item stack for the button.
     *
     * @param item the item stack
     * @return this builder
     */
    @NotNull
    MenuButtonBuilder item(@NotNull ItemStack item);

    /**
     * Sets a dynamic item supplier resolved per viewer.
     *
     * @param itemSupplier dynamic supplier function
     * @return this builder
     */
    @NotNull
    MenuButtonBuilder itemSupplier(@NotNull Function<Player, ItemStack> itemSupplier);

    /**
     * Sets the click action with full click context.
     *
     * @param onClick consumer receiving click context
     * @return this builder
     */
    @NotNull
    MenuButtonBuilder onClick(@NotNull Consumer<MenuClickContext> onClick);

    /**
     * Sets a simple runnable click callback.
     *
     * @param onClick runnable executed on click
     * @return this builder
     */
    @NotNull
    MenuButtonBuilder onClick(@NotNull Runnable onClick);

    /**
     * Configures a sound effect played upon clicking this button.
     *
     * @param sound the sound to play
     * @return this builder
     */
    @NotNull
    MenuButtonBuilder sound(@NotNull Sound sound);

    /**
     * Configures a sound effect with explicit volume and pitch.
     *
     * @param sound  the sound
     * @param volume sound volume (default 1.0)
     * @param pitch  sound pitch (default 1.0)
     * @return this builder
     */
    @NotNull
    MenuButtonBuilder sound(@NotNull Sound sound, float volume, float pitch);

    /**
     * Specifies a permission node required to click this button.
     *
     * @param permission permission string
     * @return this builder
     */
    @NotNull
    MenuButtonBuilder requirePermission(@NotNull String permission);

    /**
     * Specifies a permission node and fallback item shown when lacking permission.
     *
     * @param permission permission string
     * @param noPermItem fallback item
     * @return this builder
     */
    @NotNull
    MenuButtonBuilder requirePermission(@NotNull String permission, @Nullable ItemStack noPermItem);

    /**
     * Sets the click debounce in milliseconds to prevent double-click or macro abuse.
     *
     * @param millis debounce duration in millis
     * @return this builder
     */
    @NotNull
    MenuButtonBuilder debounce(long millis);

    /**
     * Adds an arbitrary condition that must be met for the button to be clickable.
     *
     * @param condition predicate
     * @return this builder
     */
    @NotNull
    MenuButtonBuilder condition(@NotNull Predicate<Player> condition);

    /**
     * Builds the configured {@link MenuButton}.
     *
     * @return built MenuButton
     */
    @NotNull
    MenuButton build();
}
