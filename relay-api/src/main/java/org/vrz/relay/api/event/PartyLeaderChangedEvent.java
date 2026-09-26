package org.vrz.relay.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.party.Party;

import java.util.Objects;
import java.util.UUID;

/**
 * Dispatched when party leadership is transferred.
 */
public class PartyLeaderChangedEvent extends RelayEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Party party;
    private final UUID oldLeaderId;
    private final UUID newLeaderId;

    public PartyLeaderChangedEvent(@NotNull Party party, @NotNull UUID oldLeaderId, @NotNull UUID newLeaderId) {
        super(true);
        this.party = Objects.requireNonNull(party, "party cannot be null");
        this.oldLeaderId = Objects.requireNonNull(oldLeaderId, "oldLeaderId cannot be null");
        this.newLeaderId = Objects.requireNonNull(newLeaderId, "newLeaderId cannot be null");
    }

    @NotNull
    public Party getParty() {
        return party;
    }

    @NotNull
    public UUID getOldLeaderId() {
        return oldLeaderId;
    }

    @NotNull
    public UUID getNewLeaderId() {
        return newLeaderId;
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
