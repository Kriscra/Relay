package org.vrz.relay.api.hologram;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fluent builder for instantiating and configuring modern {@link Hologram} instances.
 */
public interface HologramBuilder {

    /**
     * Sets the world spawn location for the hologram anchor.
     *
     * @param location target location
     * @return builder
     */
    @NotNull
    HologramBuilder location(@NotNull Location location);

    /**
     * Appends an Adventure {@link Component} text line.
     *
     * @param text text component
     * @return builder
     */
    @NotNull
    HologramBuilder addLine(@NotNull Component text);

    /**
     * Appends a MiniMessage formatted text line.
     *
     * @param miniMessage formatted text string
     * @return builder
     */
    @NotNull
    HologramBuilder addLine(@NotNull String miniMessage);

    /**
     * Sets the billboard camera-facing orientation.
     *
     * @param billboard billboard mode (default: {@link Display.Billboard#CENTER})
     * @return builder
     */
    @NotNull
    HologramBuilder billboard(@NotNull Display.Billboard billboard);

    /**
     * Sets the background box color behind the text.
     *
     * @param color background color (or null for default translucent black)
     * @return builder
     */
    @NotNull
    HologramBuilder backgroundColor(@Nullable Color color);

    /**
     * Sets whether text drop-shadow is rendered.
     *
     * @param shadow true for shadow
     * @return builder
     */
    @NotNull
    HologramBuilder shadow(boolean shadow);

    /**
     * Sets whether text is visible through solid blocks.
     *
     * @param seeThrough true to see through walls
     * @return builder
     */
    @NotNull
    HologramBuilder seeThrough(boolean seeThrough);

    /**
     * Sets the vertical spacing in blocks between consecutive lines.
     *
     * @param spacing spacing in blocks (default: 0.28)
     * @return builder
     */
    @NotNull
    HologramBuilder lineSpacing(double spacing);

    /**
     * Sets the owning plugin managing this hologram.
     *
     * @param plugin owner plugin
     * @return builder
     */
    @NotNull
    HologramBuilder owner(@NotNull Plugin plugin);

    /**
     * Builds the {@link Hologram} without immediately spawning its entities in the world.
     *
     * @return built hologram instance
     */
    @NotNull
    Hologram build();

    /**
     * Builds the {@link Hologram} and immediately spawns its TextDisplay entities into the world.
     *
     * @return spawned hologram instance
     */
    @NotNull
    Hologram buildAndSpawn();
}
