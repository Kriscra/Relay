package org.vrz.relay.api.scoreboard;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

/**
 * Fluent builder for constructing customizable, flicker-free {@link Sidebar} instances.
 */
public interface SidebarBuilder {

    /**
     * Sets the static title component of the sidebar.
     *
     * @param title title component
     * @return this builder
     */
    @NotNull
    SidebarBuilder title(@NotNull Component title);

    /**
     * Sets the title using a MiniMessage format string.
     *
     * @param miniMessage MiniMessage string
     * @return this builder
     */
    @NotNull
    SidebarBuilder titleMiniMessage(@NotNull String miniMessage);

    /**
     * Sets an animated title cycling through a sequence of frames.
     *
     * @param frames        list of title frames
     * @param intervalTicks tick interval between frames
     * @return this builder
     */
    @NotNull
    SidebarBuilder animatedTitle(@NotNull List<Component> frames, long intervalTicks);

    /**
     * Sets a static component at the specified line index (1 to 15).
     *
     * @param lineIndex 1-based line index
     * @param text      component text
     * @return this builder
     */
    @NotNull
    SidebarBuilder line(int lineIndex, @NotNull Component text);

    /**
     * Sets a line using a MiniMessage format string.
     *
     * @param lineIndex   1-based line index
     * @param miniMessage MiniMessage string
     * @return this builder
     */
    @NotNull
    SidebarBuilder lineMiniMessage(int lineIndex, @NotNull String miniMessage);

    /**
     * Binds a dynamic component supplier for the specified line index.
     * Re-evaluated automatically on every tick refresh.
     *
     * @param lineIndex 1-based line index
     * @param supplier  component supplier
     * @return this builder
     */
    @NotNull
    SidebarBuilder line(int lineIndex, @NotNull Supplier<Component> supplier);

    /**
     * Populates all lines sequentially from a list of components (top to bottom).
     *
     * @param lines list of line components
     * @return this builder
     */
    @NotNull
    SidebarBuilder lines(@NotNull List<Component> lines);

    /**
     * Populates all lines sequentially using MiniMessage format strings (top to bottom).
     *
     * @param lines list of MiniMessage strings
     * @return this builder
     */
    @NotNull
    SidebarBuilder linesMiniMessage(@NotNull List<String> lines);

    /**
     * Sets the automatic background update interval for dynamic line suppliers and animations.
     * A value of 0 or negative disables automatic updates.
     *
     * @param intervalTicks tick interval (e.g. 20L = 1 second)
     * @return this builder
     */
    @NotNull
    SidebarBuilder updateInterval(long intervalTicks);

    /**
     * Sets the priority of this sidebar. Higher priority sidebars take precedence
     * over default or lower-priority sidebars for a player.
     *
     * @param priority priority integer (higher = more important)
     * @return this builder
     */
    @NotNull
    SidebarBuilder priority(int priority);

    /**
     * Builds the {@link Sidebar} and immediately displays it to the target player.
     *
     * @param player player to show
     * @return created active sidebar
     */
    @NotNull
    Sidebar buildAndShow(@NotNull Player player);

    /**
     * Builds the {@link Sidebar} without immediately displaying it.
     *
     * @return created sidebar
     */
    @NotNull
    Sidebar build();
}
