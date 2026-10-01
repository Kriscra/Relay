package org.vrz.relay.api.scoreboard;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Represents a virtual, flicker-free sidebar (scoreboard) rendered for a player.
 * <p>
 * Supports 1-15 dynamic or static lines, MiniMessage rich components,
 * animated title frames, dynamic lambda suppliers, and automated tick-based refresh loops.
 * Thread-safe and Folia multi-threading compliant.
 */
public interface Sidebar {

    /**
     * Gets the unique identifier of this sidebar instance.
     *
     * @return sidebar UUID
     */
    @NotNull
    UUID getId();

    /**
     * Gets the plugin that created and owns this sidebar.
     *
     * @return owning plugin
     */
    @NotNull
    Plugin getPlugin();

    /**
     * Gets the player currently viewing this sidebar, or {@code null} if shared.
     *
     * @return viewing player
     */
    @Nullable
    Player getPlayer();

    /**
     * Gets all players currently receiving this sidebar.
     *
     * @return collection of viewer players
     */
    @NotNull
    Collection<Player> getViewers();

    /**
     * Gets the current title component.
     *
     * @return title component
     */
    @NotNull
    Component getTitle();

    /**
     * Sets the title component of the sidebar.
     *
     * @param title new title
     */
    void setTitle(@NotNull Component title);

    /**
     * Sets the title using a MiniMessage format string.
     *
     * @param miniMessage MiniMessage string
     */
    void setTitleMiniMessage(@NotNull String miniMessage);

    /**
     * Configures an animated title cycling through a list of component frames.
     *
     * @param frames        frames to cycle through
     * @param intervalTicks tick interval between frames
     */
    void setAnimatedTitle(@NotNull List<Component> frames, long intervalTicks);

    /**
     * Gets an unmodifiable map of current line indices (1 to 15) to their rendered components.
     *
     * @return map of line index to component
     */
    @NotNull
    Map<Integer, Component> getLines();

    /**
     * Gets the rendered component at the specified line index.
     *
     * @param lineIndex 1-based line index (1 to 15)
     * @return rendered component, or {@code null} if empty
     */
    @Nullable
    Component getLine(int lineIndex);

    /**
     * Sets a static component at the specified line index (1 to 15).
     *
     * @param lineIndex 1-based line index
     * @param text      component text
     */
    void setLine(int lineIndex, @NotNull Component text);

    /**
     * Sets a line using a MiniMessage format string.
     *
     * @param lineIndex   1-based line index
     * @param miniMessage MiniMessage string
     */
    void setLineMiniMessage(int lineIndex, @NotNull String miniMessage);

    /**
     * Binds a dynamic supplier for the specified line index.
     * Evaluated automatically on every update cycle.
     *
     * @param lineIndex 1-based line index
     * @param supplier  component supplier
     */
    void setLineSupplier(int lineIndex, @NotNull Supplier<Component> supplier);

    /**
     * Replaces all lines sequentially from a list of components (top to bottom).
     *
     * @param lines list of line components
     */
    void setLines(@NotNull List<Component> lines);

    /**
     * Replaces all lines sequentially using MiniMessage format strings (top to bottom).
     *
     * @param lines list of MiniMessage strings
     */
    void setLinesMiniMessage(@NotNull List<String> lines);

    /**
     * Removes the line at the specified index.
     *
     * @param lineIndex 1-based line index
     */
    void removeLine(int lineIndex);

    /**
     * Clears all lines from the sidebar.
     */
    void clearLines();

    /**
     * Forces an immediate refresh and re-evaluation of all dynamic line suppliers and titles.
     */
    void update();

    /**
     * Checks if this sidebar has been destroyed.
     *
     * @return true if destroyed
     */
    boolean isDestroyed();

    /**
     * Destroys this sidebar, unregisters all internal teams and objectives,
     * stops background update tasks, and restores the viewer's main scoreboard.
     */
    void destroy();

    /**
     * Shows this sidebar to the specified player.
     *
     * @param player player to show
     */
    void show(@NotNull Player player);

    /**
     * Hides this sidebar from the specified player.
     *
     * @param player player to hide from
     */
    void hide(@NotNull Player player);
}
