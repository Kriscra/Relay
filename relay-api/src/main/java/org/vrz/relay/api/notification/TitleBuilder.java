package org.vrz.relay.api.notification;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Fluent builder for constructing and enqueueing Title notifications.
 */
public interface TitleBuilder {

    /**
     * Sets the main title using a Kyori Adventure {@link Component}.
     *
     * @param title main title
     * @return builder instance
     */
    @NotNull
    TitleBuilder title(@NotNull Component title);

    /**
     * Sets the main title using a MiniMessage format string.
     *
     * @param miniMessage MiniMessage string
     * @return builder instance
     */
    @NotNull
    TitleBuilder titleMiniMessage(@NotNull String miniMessage);

    /**
     * Sets the subtitle using a Kyori Adventure {@link Component}.
     *
     * @param subtitle subtitle
     * @return builder instance
     */
    @NotNull
    TitleBuilder subtitle(@NotNull Component subtitle);

    /**
     * Sets the subtitle using a MiniMessage format string.
     *
     * @param miniMessage MiniMessage string
     * @return builder instance
     */
    @NotNull
    TitleBuilder subtitleMiniMessage(@NotNull String miniMessage);

    /**
     * Sets the priority of this title.
     * Defaults to {@link NotificationPriority#NORMAL}.
     *
     * @param priority priority tier
     * @return builder instance
     */
    @NotNull
    TitleBuilder priority(@NotNull NotificationPriority priority);

    /**
     * Sets the timing for fade-in, stay, and fade-out durations.
     *
     * @param fadeIn  fade in duration
     * @param stay    stay duration
     * @param fadeOut fade out duration
     * @return builder instance
     */
    @NotNull
    TitleBuilder times(@NotNull Duration fadeIn, @NotNull Duration stay, @NotNull Duration fadeOut);

    /**
     * Sets an Adventure sound cue to play when the title is shown.
     *
     * @param sound adventure sound
     * @return builder instance
     */
    @NotNull
    TitleBuilder sound(@NotNull Sound sound);

    /**
     * Sets a Bukkit sound cue to play when the title is shown.
     *
     * @param sound  bukkit sound
     * @param volume volume level
     * @param pitch  pitch level
     * @return builder instance
     */
    @NotNull
    TitleBuilder sound(@NotNull org.bukkit.Sound sound, float volume, float pitch);

    /**
     * Sets a callback to execute when the title finishes displaying.
     *
     * @param onComplete runnable callback
     * @return builder instance
     */
    @NotNull
    TitleBuilder onComplete(@NotNull Runnable onComplete);

    /**
     * Enqueues and dispatches this title notification.
     *
     * @param plugin owning plugin
     * @return created notification handle
     */
    @NotNull
    TitleNotification send(@NotNull Plugin plugin);
}
