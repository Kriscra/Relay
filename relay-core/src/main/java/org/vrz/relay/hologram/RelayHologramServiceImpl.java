package org.vrz.relay.hologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.event.HologramCreatedEvent;
import org.vrz.relay.api.event.HologramDeletedEvent;
import org.vrz.relay.api.hologram.Hologram;
import org.vrz.relay.api.hologram.HologramBuilder;
import org.vrz.relay.api.hologram.HologramService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe implementation of {@link HologramService}.
 */
public class RelayHologramServiceImpl implements HologramService {

    private final ConcurrentHashMap<String, Hologram> holograms = new ConcurrentHashMap<>();

    @Override
    @NotNull
    public HologramBuilder createBuilder(@NotNull String id) {
        Objects.requireNonNull(id, "id cannot be null");
        return new RelayHologramBuilderImpl(this, id);
    }

    @Override
    @NotNull
    public Hologram createHologram(@NotNull String id, @NotNull Location location, @NotNull Plugin owner) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(owner, "owner cannot be null");

        RelayHologramImpl holo = new RelayHologramImpl(id, location, owner);
        registerHologram(holo);
        return holo;
    }

    void registerHologram(@NotNull Hologram hologram) {
        Hologram existing = holograms.put(hologram.getId(), hologram);
        if (existing != null && existing.isSpawned()) {
            existing.remove();
        }

        try {
            if (Bukkit.getServer() != null) {
                Bukkit.getPluginManager().callEvent(new HologramCreatedEvent(hologram));
            }
        } catch (Throwable ignored) {}
    }

    @Override
    @NotNull
    public Optional<Hologram> getHologram(@NotNull String id) {
        Objects.requireNonNull(id, "id cannot be null");
        return Optional.ofNullable(holograms.get(id));
    }

    @Override
    @NotNull
    public Collection<Hologram> getAllHolograms() {
        return Collections.unmodifiableCollection(holograms.values());
    }

    @Override
    @NotNull
    public Collection<Hologram> getHologramsByPlugin(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        return holograms.values().stream()
                .filter(h -> h.getOwner().equals(plugin))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public boolean deleteHologram(@NotNull String id) {
        Objects.requireNonNull(id, "id cannot be null");
        Hologram holo = holograms.remove(id);
        if (holo != null) {
            holo.remove();
            try {
                if (Bukkit.getServer() != null) {
                    Bukkit.getPluginManager().callEvent(new HologramDeletedEvent(holo));
                }
            } catch (Throwable ignored) {}
            return true;
        }
        return false;
    }

    @Override
    public void deleteAll(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        List<String> toRemove = holograms.values().stream()
                .filter(h -> h.getOwner().equals(plugin))
                .map(Hologram::getId)
                .toList();

        for (String id : toRemove) {
            deleteHologram(id);
        }
    }

    @Override
    public void deleteAll() {
        for (Hologram holo : holograms.values()) {
            holo.remove();
        }
        holograms.clear();
    }

    @Override
    public int getActiveHologramCount() {
        return holograms.size();
    }
}
