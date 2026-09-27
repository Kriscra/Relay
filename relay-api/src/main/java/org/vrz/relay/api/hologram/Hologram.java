package org.vrz.relay.api.hologram;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a high-performance floating holographic display backed by
 * modern Minecraft 1.20+ / 1.21+ {@link org.bukkit.entity.TextDisplay} entities.
 * <p>
 * Eliminates invisible ArmorStand entity lag, zero network collision overhead,
 * and provides full Folia region thread safety.
 */
public interface Hologram {

    /**
     * Gets the unique string identifier for this hologram.
     *
     * @return unique hologram id
     */
    @NotNull
    String getId();

    /**
     * Gets the current base anchor location in the world.
     *
     * @return anchor location
     */
    @NotNull
    Location getLocation();

    /**
     * Teleports the hologram anchor and all its associated lines to a new location.
     *
     * @param location target location
     * @return future completed when all display entities have moved
     */
    @NotNull
    CompletableFuture<Void> setLocationAsync(@NotNull Location location);

    /**
     * Synchronous location update (delegates to region scheduler if running on Folia).
     *
     * @param location target location
     */
    void setLocation(@NotNull Location location);

    /**
     * Gets an unmodifiable view of all lines in this hologram, ordered top-to-bottom.
     *
     * @return list of lines
     */
    @NotNull
    List<HologramLine> getLines();

    /**
     * Gets a specific line by its 0-based index.
     *
     * @param index line index
     * @return hologram line
     * @throws IndexOutOfBoundsException if index is invalid
     */
    @NotNull
    HologramLine getLine(int index);

    /**
     * Appends a new line of text to the bottom of the hologram.
     *
     * @param text adventure component
     * @return the created line
     */
    @NotNull
    HologramLine appendLine(@NotNull Component text);

    /**
     * Appends a new line of text using MiniMessage formatting.
     *
     * @param miniMessage formatted text string
     * @return the created line
     */
    @NotNull
    HologramLine appendLine(@NotNull String miniMessage);

    /**
     * Inserts a line of text at a specific index, shifting subsequent lines down.
     *
     * @param index target index
     * @param text  adventure component
     * @return the created line
     */
    @NotNull
    HologramLine insertLine(int index, @NotNull Component text);

    /**
     * Replaces the text at a specific line index.
     *
     * @param index target index
     * @param text  new text component
     */
    void setLine(int index, @NotNull Component text);

    /**
     * Removes the line at the specified index.
     *
     * @param index line index to remove
     */
    void removeLine(int index);

    /**
     * Clears all lines from the hologram.
     */
    void clearLines();

    /**
     * Gets the total number of lines in this hologram.
     *
     * @return line count
     */
    int getLineCount();

    /**
     * Gets the vertical spacing between individual lines in blocks.
     *
     * @return line spacing in blocks (default ~0.28)
     */
    double getLineSpacing();

    /**
     * Sets the vertical spacing between lines.
     *
     * @param spacing spacing in blocks
     */
    void setLineSpacing(double spacing);

    /**
     * Gets the current billboard orientation mode (how the text rotates towards players).
     *
     * @return billboard mode
     */
    @NotNull
    Display.Billboard getBillboard();

    /**
     * Sets the billboard orientation mode (e.g. {@link Display.Billboard#CENTER}, {@link Display.Billboard#VERTICAL}).
     *
     * @param billboard billboard mode
     */
    void setBillboard(@NotNull Display.Billboard billboard);

    /**
     * Gets the background color behind the text.
     *
     * @return background color, or null if default transparent dark
     */
    @Nullable
    Color getBackgroundColor();

    /**
     * Sets the background box color behind the text.
     *
     * @param color background color
     */
    void setBackgroundColor(@Nullable Color color);

    /**
     * Checks if text drop-shadow is enabled.
     *
     * @return true if text has shadow
     */
    boolean hasTextShadow();

    /**
     * Enables or disables text drop-shadow rendering.
     *
     * @param shadow true to enable shadow
     */
    void setTextShadow(boolean shadow);

    /**
     * Checks if the text can be seen through solid walls.
     *
     * @return true if see-through
     */
    boolean isSeeThrough();

    /**
     * Sets whether the hologram can be seen through blocks.
     *
     * @param seeThrough true to see through walls
     */
    void setSeeThrough(boolean seeThrough);

    /**
     * Gets the owning plugin that created and manages this hologram.
     *
     * @return owner plugin
     */
    @NotNull
    Plugin getOwner();

    /**
     * Checks if the hologram is currently spawned and visible in the world.
     *
     * @return true if spawned
     */
    boolean isSpawned();

    /**
     * Asynchronously spawns all TextDisplay entities in the appropriate Folia region thread.
     *
     * @return future completed when spawn completes
     */
    @NotNull
    CompletableFuture<Void> spawnAsync();

    /**
     * Synchronously spawns the hologram entities.
     */
    void spawn();

    /**
     * Asynchronously removes all TextDisplay entities from the world.
     *
     * @return future completed when removal completes
     */
    @NotNull
    CompletableFuture<Void> removeAsync();

    /**
     * Synchronously removes the hologram entities.
     */
    void remove();
}
