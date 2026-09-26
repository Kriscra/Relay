package org.vrz.relay.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.party.Party;

import java.util.Objects;
import java.util.UUID;

/**
 * Dispatched when an existing party is disbanded.
 */
public class PartyDisbandedEvent extends RelayEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Party party;
    private final UUID disbanderId;

    public PartyDisbandedEvent(@NotNull Party party, @Nullable UUID disbanderId) {
        super(true);
        this.party = Objects.requireNonNull(party, "party cannot be null");
        this.disbanderId = disbanderId;
    }

    @NotNull
    public Party getParty() {
        return party;
    }

    @Nullable
    public UUID getDisbanderId() {
        return disbanderId;
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
