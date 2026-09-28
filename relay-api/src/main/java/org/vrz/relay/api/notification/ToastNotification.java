package org.vrz.relay.api.notification;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a virtual toast / advancement popup notification.
 */
public interface ToastNotification {

    /**
     * Gets the plugin that scheduled this toast.
     *
     * @return owning plugin
     */
    @NotNull
    Plugin getPlugin();

    /**
     * Gets the target player receiving this toast.
     *
     * @return target player
     */
    @NotNull
    Player getPlayer();

    /**
     * Gets the toast title component.
     *
     * @return title
     */
    @NotNull
    Component getTitle();

    /**
     * Gets the toast description component.
     *
     * @return description
     */
    @NotNull
    Component getDescription();

    /**
     * Gets the icon material displayed in the toast.
     *
     * @return icon material
     */
    @NotNull
    Material getIcon();

    /**
     * Gets the frame style of the toast.
     *
     * @return frame style
     */
    @NotNull
    ToastFrame getFrame();

    /**
     * Gets the sound cue played when the toast displays.
     *
     * @return sound cue or null
     */
    @Nullable
    Sound getSound();
}
