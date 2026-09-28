package org.vrz.relay.api.notification;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Fluent builder for constructing and enqueueing ActionBar notifications.
 */
public interface ActionBarBuilder {

    /**
     * Sets the action bar message using a Kyori Adventure {@link Component}.
     *
     * @param message component message
     * @return builder instance
     */
    @NotNull
    ActionBarBuilder message(@NotNull Component message);

    /**
     * Sets the action bar message using a MiniMessage format string.
     *
     * @param miniMessage MiniMessage string
     * @return builder instance
     */
    @NotNull
    ActionBarBuilder messageMiniMessage(@NotNull String miniMessage);

    /**
     * Sets the priority of this notification.
     * Defaults to {@link NotificationPriority#NORMAL}.
     *
     * @param priority priority tier
     * @return builder instance
     */
    @NotNull
    ActionBarBuilder priority(@NotNull NotificationPriority priority);

    /**
     * Sets the duration this notification should remain displayed.
     * Defaults to 3 seconds.
     *
     * @param duration display duration
     * @return builder instance
     */
    @NotNull
    ActionBarBuilder duration(@NotNull Duration duration);

    /**
     * Sets an Adventure sound cue to play when the notification is shown.
     *
     * @param sound adventure sound
     * @return builder instance
     */
    @NotNull
    ActionBarBuilder sound(@NotNull Sound sound);

    /**
     * Sets a Bukkit sound cue to play when the notification is shown.
     *
     * @param sound  bukkit sound
     * @param volume volume level
     * @param pitch  pitch level
     * @return builder instance
     */
    @NotNull
    ActionBarBuilder sound(@NotNull org.bukkit.Sound sound, float volume, float pitch);

    /**
     * Sets a callback to execute when the notification expires or finishes displaying.
     *
     * @param onComplete runnable callback
     * @return builder instance
     */
    @NotNull
    ActionBarBuilder onComplete(@NotNull Runnable onComplete);

    /**
     * Enqueues and dispatches this action bar notification.
     *
     * @param plugin owning plugin
     * @return created notification handle
     */
    @NotNull
    ActionBarNotification send(@NotNull Plugin plugin);
}
