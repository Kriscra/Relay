package org.vrz.relay.api.hologram;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

/**
 * Universal, high-performance hologram service for Relay.
 * <p>
 * Manages floating 3D text displays using modern Minecraft {@link org.bukkit.entity.TextDisplay}
 * entities with zero ArmorStand entity-lag, complete Folia region safety, and automatic
 * zero-leak memory lifecycle.
 */
public interface HologramService {

    /**
     * Creates a new fluent builder for constructing a custom {@link Hologram}.
     *
     * @param id unique identifier for the hologram
     * @return hologram builder
     */
    @NotNull
    HologramBuilder createBuilder(@NotNull String id);

    /**
     * Creates and registers a new empty hologram at the specified location.
     *
     * @param id       unique identifier
     * @param location world anchor location
     * @param owner    owning plugin
     * @return created hologram instance
     */
    @NotNull
    Hologram createHologram(@NotNull String id, @NotNull Location location, @NotNull Plugin owner);

    /**
     * Retrieves an existing hologram by its unique identifier.
     *
     * @param id hologram id
     * @return optional containing the hologram if present
     */
    @NotNull
    Optional<Hologram> getHologram(@NotNull String id);

    /**
     * Gets all currently registered holograms across all plugins.
     *
     * @return unmodifiable collection of all holograms
     */
    @NotNull
    Collection<Hologram> getAllHolograms();

    /**
     * Gets all holograms registered by a specific plugin.
     *
     * @param plugin owning plugin
     * @return collection of holograms owned by the plugin
     */
    @NotNull
    Collection<Hologram> getHologramsByPlugin(@NotNull Plugin plugin);

    /**
     * Deletes and removes a hologram from the world.
     *
     * @param id hologram id
     * @return true if hologram was found and removed, false otherwise
     */
    boolean deleteHologram(@NotNull String id);

    /**
     * Deletes and cleans up all holograms created by a specific plugin.
     * Called automatically during {@link org.bukkit.event.server.PluginDisableEvent} to guarantee zero memory leaks.
     *
     * @param plugin target plugin
     */
    void deleteAll(@NotNull Plugin plugin);

    /**
     * Deletes all holograms on the server.
     */
    void deleteAll();

    /**
     * Gets the total number of currently registered holograms.
     *
     * @return active hologram count
     */
    int getActiveHologramCount();
}
