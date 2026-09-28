package org.vrz.relay.api.notification;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * Represents an active or queued ActionBar notification instance.
 */
public interface ActionBarNotification {

    /**
     * Gets the plugin that scheduled this notification.
     *
     * @return owning plugin
     */
    @NotNull
    Plugin getPlugin();

    /**
     * Gets the target player receiving this notification.
     *
     * @return target player
     */
    @NotNull
    Player getPlayer();

    /**
     * Gets the component message to display in the action bar.
     *
     * @return message component
     */
    @NotNull
    Component getMessage();

    /**
     * Gets the priority tier of this notification.
     *
     * @return priority
     */
    @NotNull
    NotificationPriority getPriority();

    /**
     * Gets the total duration this notification should remain on screen.
     *
     * @return duration
     */
    @NotNull
    Duration getDuration();

    /**
     * Gets the audio cue played when this notification is first shown.
     *
     * @return sound cue or null
     */
    @Nullable
    Sound getSound();

    /**
     * Gets the remaining duration in milliseconds.
     *
     * @return remaining millis
     */
    long getRemainingMillis();

    /**
     * Checks if this notification has expired.
     *
     * @return true if expired
     */
    boolean isExpired();

    /**
     * Manually cancels this notification, removing it from queue or active display.
     */
    void cancel();
}
