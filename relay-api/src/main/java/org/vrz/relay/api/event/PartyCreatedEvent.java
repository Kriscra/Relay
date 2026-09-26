package org.vrz.relay.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.party.Party;

import java.util.Objects;

/**
 * Dispatched when a new party is created.
 */
public class PartyCreatedEvent extends RelayEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Party party;

    public PartyCreatedEvent(@NotNull Party party) {
        super(true);
        this.party = Objects.requireNonNull(party, "party cannot be null");
    }

    @NotNull
    public Party getParty() {
        return party;
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
