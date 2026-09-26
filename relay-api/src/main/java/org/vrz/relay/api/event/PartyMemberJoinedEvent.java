package org.vrz.relay.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.party.Party;

import java.util.Objects;
import java.util.UUID;

/**
 * Dispatched when a player joins an active party.
 */
public class PartyMemberJoinedEvent extends RelayEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Party party;
    private final UUID memberId;

    public PartyMemberJoinedEvent(@NotNull Party party, @NotNull UUID memberId) {
        super(true);
        this.party = Objects.requireNonNull(party, "party cannot be null");
        this.memberId = Objects.requireNonNull(memberId, "memberId cannot be null");
    }

    @NotNull
    public Party getParty() {
        return party;
    }

    @NotNull
    public UUID getMemberId() {
        return memberId;
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
