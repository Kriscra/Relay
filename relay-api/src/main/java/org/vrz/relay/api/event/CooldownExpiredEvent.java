package org.vrz.relay.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.cooldown.CooldownEntry;

import java.util.UUID;

/**
 * Dispatched when an active cooldown reaches expiration.
 */
public class CooldownExpiredEvent extends RelayEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final CooldownEntry entry;

    public CooldownExpiredEvent(@NotNull CooldownEntry entry) {
        super(true); // Asynchronously dispatched
        this.entry = entry;
    }

    @NotNull
    public CooldownEntry getEntry() {
        return entry;
    }

    @Nullable
    public UUID getTargetId() {
        return entry.targetId();
    }

    @NotNull
    public String getKey() {
        return entry.key();
    }

    public boolean isGlobal() {
        return entry.isGlobal();
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
