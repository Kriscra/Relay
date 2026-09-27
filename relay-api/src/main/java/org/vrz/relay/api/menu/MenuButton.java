package org.vrz.relay.api.menu;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Represents an interactive or static button placed in a Relay virtual menu slot.
 */
public interface MenuButton {

    /**
     * Gets the item stack representing this button for the given player.
     * Can return dynamic lore, amounts, or textures depending on the viewing player.
     *
     * @param player viewing player
     * @return the item stack to render
     */
    @Nullable
    ItemStack getItem(@NotNull Player player);

    /**
     * Gets the click handler executed when a player interacts with this button.
     *
     * @return consumer accepting click context
     */
    @NotNull
    Consumer<MenuClickContext> getClickHandler();

    /**
     * Optional sound played upon clicking this button.
     *
     * @return sound or null if none
     */
    @Nullable
    Sound getClickSound();

    /**
     * Volume for the click sound.
     *
     * @return volume float
     */
    float getSoundVolume();

    /**
     * Pitch for the click sound.
     *
     * @return pitch float
     */
    float getSoundPitch();

    /**
     * Optional permission node required to click this button.
     *
     * @return permission string or null
     */
    @Nullable
    String getRequiredPermission();

    /**
     * Fallback item displayed if the player lacks the required permission.
     *
     * @return fallback item stack or null
     */
    @Nullable
    ItemStack getNoPermissionItem();

    /**
     * Debounce duration in milliseconds to prevent rapid macro/auto-click spamming.
     *
     * @return debounce millis (0 to disable)
     */
    long getDebounceMillis();

    /**
     * Checks if the given player is eligible to click this button (evaluates permissions and conditions).
     *
     * @param player the player
     * @return true if clickable
     */
    boolean canClick(@NotNull Player player);

    /**
     * Creates a static, non-clickable button showing the given item.
     *
     * @param item the item stack
     * @return static MenuButton
     */
    static MenuButton of(@NotNull ItemStack item) {
        return builder().item(item).build();
    }

    /**
     * Creates a clickable button with a click handler.
     *
     * @param item    the item stack
     * @param onClick click consumer
     * @return clickable MenuButton
     */
    static MenuButton of(@NotNull ItemStack item, @NotNull Consumer<MenuClickContext> onClick) {
        return builder().item(item).onClick(onClick).build();
    }

    /**
     * Creates a clickable button with a runnable callback.
     *
     * @param item    the item stack
     * @param onClick runnable callback
     * @return clickable MenuButton
     */
    static MenuButton of(@NotNull ItemStack item, @NotNull Runnable onClick) {
        return builder().item(item).onClick(onClick).build();
    }

    /**
     * Starts building a new button via MenuButtonBuilder.
     *
     * @return new MenuButtonBuilder
     */
    static MenuButtonBuilder builder() {
        return org.vrz.relay.api.RelayAPI.getMenus().createButtonBuilder();
    }
}
