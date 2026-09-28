package org.vrz.relay.api.notification;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * Represents an active or queued Title notification instance.
 */
public interface TitleNotification {

    /**
     * Gets the plugin that scheduled this title.
     *
     * @return owning plugin
     */
    @NotNull
    Plugin getPlugin();

    /**
     * Gets the target player receiving this title.
     *
     * @return target player
     */
    @NotNull
    Player getPlayer();

    /**
     * Gets the main title component.
     *
     * @return main title
     */
    @NotNull
    Component getTitle();

    /**
     * Gets the subtitle component.
     *
     * @return subtitle
     */
    @NotNull
    Component getSubtitle();

    /**
     * Gets the priority tier of this title.
     *
     * @return priority
     */
    @NotNull
    NotificationPriority getPriority();

    /**
     * Gets the fade-in duration.
     *
     * @return fade in
     */
    @NotNull
    Duration getFadeIn();

    /**
     * Gets the stay duration.
     *
     * @return stay
     */
    @NotNull
    Duration getStay();

    /**
     * Gets the fade-out duration.
     *
     * @return fade out
     */
    @NotNull
    Duration getFadeOut();

    /**
     * Gets the audio cue played when this title is shown.
     *
     * @return sound cue or null
     */
    @Nullable
    Sound getSound();

    /**
     * Checks if this title has expired.
     *
     * @return true if expired
     */
    boolean isExpired();

    /**
     * Cancels this title notification and removes it from display or queue.
     */
    void cancel();
}
