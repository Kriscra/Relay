package org.vrz.relay.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.hologram.Hologram;

/**
 * Dispatched when a new {@link Hologram} is registered and created.
 */
public class HologramCreatedEvent extends RelayEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Hologram hologram;

    public HologramCreatedEvent(@NotNull Hologram hologram) {
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
