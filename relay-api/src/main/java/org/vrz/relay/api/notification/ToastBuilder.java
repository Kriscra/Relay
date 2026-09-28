package org.vrz.relay.api.notification;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Fluent builder for creating and displaying virtual toast notifications.
 */
public interface ToastBuilder {

    /**
     * Sets the toast title using a Kyori Adventure {@link Component}.
     *
     * @param title title
     * @return builder instance
     */
    @NotNull
    ToastBuilder title(@NotNull Component title);

    /**
     * Sets the toast title using a MiniMessage format string.
     *
     * @param miniMessage MiniMessage string
     * @return builder instance
     */
    @NotNull
    ToastBuilder titleMiniMessage(@NotNull String miniMessage);

    /**
     * Sets the toast description using a Kyori Adventure {@link Component}.
     *
     * @param description description
     * @return builder instance
     */
    @NotNull
    ToastBuilder description(@NotNull Component description);

    /**
     * Sets the toast description using a MiniMessage format string.
     *
     * @param miniMessage MiniMessage string
     * @return builder instance
     */
    @NotNull
    ToastBuilder descriptionMiniMessage(@NotNull String miniMessage);

    /**
     * Sets the icon item material displayed in the popup.
     * Defaults to {@link Material#KNOWLEDGE_BOOK}.
     *
     * @param icon icon material
     * @return builder instance
     */
    @NotNull
    ToastBuilder icon(@NotNull Material icon);

    /**
     * Sets the frame border style of the toast.
     * Defaults to {@link ToastFrame#TASK}.
     *
     * @param frame frame style
     * @return builder instance
     */
    @NotNull
    ToastBuilder frame(@NotNull ToastFrame frame);

    /**
     * Sets an Adventure sound cue to play when the toast is displayed.
     *
     * @param sound adventure sound
     * @return builder instance
     */
    @NotNull
    ToastBuilder sound(@NotNull Sound sound);

    /**
     * Sets a Bukkit sound cue to play when the toast is displayed.
     *
     * @param sound  bukkit sound
     * @param volume volume level
     * @param pitch  pitch level
     * @return builder instance
     */
    @NotNull
    ToastBuilder sound(@NotNull org.bukkit.Sound sound, float volume, float pitch);

    /**
     * Sends the virtual toast popup to the target player.
     *
     * @param plugin owning plugin
     * @return created toast notification
     */
    @NotNull
    ToastNotification send(@NotNull Plugin plugin);
}
