package org.vrz.relay.api.notification;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Fluent builder for creating and displaying animated or static BossBars.
 */
public interface BossBarBuilder {

    /**
     * Sets the boss bar title using a {@link Component}.
     * Supports placeholders %time% (seconds left) and %progress% (percentage).
     *
     * @param title component title
     * @return builder instance
     */
    @NotNull
    BossBarBuilder title(@NotNull Component title);

    /**
     * Sets the boss bar title using a MiniMessage format string.
     * Supports placeholders %time% (seconds left) and %progress% (percentage).
     *
     * @param miniMessage MiniMessage string
     * @return builder instance
     */
    @NotNull
    BossBarBuilder titleMiniMessage(@NotNull String miniMessage);

    /**
     * Sets the boss bar color.
     * Defaults to {@link BossBar.Color#PURPLE}.
     *
     * @param color boss bar color
     * @return builder instance
     */
    @NotNull
    BossBarBuilder color(@NotNull BossBar.Color color);

    /**
     * Sets the boss bar overlay style.
     * Defaults to {@link BossBar.Overlay#PROGRESS}.
     *
     * @param overlay boss bar overlay
     * @return builder instance
     */
    @NotNull
    BossBarBuilder overlay(@NotNull BossBar.Overlay overlay);

    /**
     * Sets a static progress value between 0.0f and 1.0f.
     *
     * @param progress static progress
     * @return builder instance
     */
    @NotNull
    BossBarBuilder progress(float progress);

    /**
     * Configures the boss bar as an animated countdown from 1.0f to 0.0f over the specified duration.
     *
     * @param duration total countdown duration
     * @return builder instance
     */
    @NotNull
    BossBarBuilder countdown(@NotNull Duration duration);

    /**
     * Configures the boss bar as an animated count-up from 0.0f to 1.0f over the specified duration.
     *
     * @param duration total countup duration
     * @return builder instance
     */
    @NotNull
    BossBarBuilder countup(@NotNull Duration duration);

    /**
     * Sets a callback to execute when the countdown or duration completes.
     *
     * @param onComplete runnable callback
     * @return builder instance
     */
    @NotNull
    BossBarBuilder onComplete(@NotNull Runnable onComplete);

    /**
     * Builds, shows, and starts tracking this BossBar.
     *
     * @param plugin owning plugin
     * @return active boss bar handle
     */
    @NotNull
    ActiveBossBar send(@NotNull Plugin plugin);
}
