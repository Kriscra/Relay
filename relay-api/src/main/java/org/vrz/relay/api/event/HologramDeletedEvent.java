package org.vrz.relay.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.hologram.Hologram;

/**
 * Dispatched when a {@link Hologram} is deleted and removed from the world.
 */
public class HologramDeletedEvent extends RelayEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Hologram hologram;

    public HologramDeletedEvent(@NotNull Hologram hologram) {
        super(false);
        this.hologram = hologram;
    }

    @NotNull
    public Hologram getHologram() {
        return hologram;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
