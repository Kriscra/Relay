package org.vrz.relay.api.notification;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Handle to an actively running or countdown BossBar instance.
 */
public interface ActiveBossBar {

    /**
     * Gets the plugin that created this boss bar.
     *
     * @return owning plugin
     */
    @NotNull
    Plugin getPlugin();

    /**
     * Gets the target player viewing this boss bar.
     *
     * @return target player
     */
    @NotNull
    Player getPlayer();

    /**
     * Gets the underlying Kyori Adventure {@link BossBar}.
     *
     * @return adventure boss bar
     */
    @NotNull
    BossBar getAdventureBar();

    /**
     * Updates the boss bar title using a {@link Component}.
     *
     * @param title new title
     */
    void setTitle(@NotNull Component title);

    /**
     * Updates the boss bar title using a MiniMessage format string.
     *
     * @param miniMessage new MiniMessage title
     */
    void setTitleMiniMessage(@NotNull String miniMessage);

    /**
     * Updates the progress of the boss bar (clamped between 0.0f and 1.0f).
     *
     * @param progress new progress
     */
    void setProgress(float progress);

    /**
     * Gets the current progress (between 0.0f and 1.0f).
     *
     * @return current progress
     */
    float getProgress();

    /**
     * Updates the boss bar color.
     *
     * @param color new color
     */
    void setColor(@NotNull BossBar.Color color);

    /**
     * Gets the remaining duration in milliseconds if this is a countdown bar.
     *
     * @return remaining millis, or -1 if static
     */
    long getRemainingMillis();

    /**
     * Checks if this boss bar is active and visible.
     *
     * @return true if visible
     */
    boolean isVisible();

    /**
     * Dismisses and removes the boss bar from the player.
     */
    void dismiss();
}
