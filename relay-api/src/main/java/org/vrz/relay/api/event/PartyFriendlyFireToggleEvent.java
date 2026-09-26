package org.vrz.relay.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.party.Party;

import java.util.Objects;

/**
 * Dispatched when a party's friendly fire setting is changed.
 */
public class PartyFriendlyFireToggleEvent extends RelayEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Party party;
    private final boolean friendlyFireEnabled;

    public PartyFriendlyFireToggleEvent(@NotNull Party party, boolean friendlyFireEnabled) {
        super(true);
        this.party = Objects.requireNonNull(party, "party cannot be null");
        this.friendlyFireEnabled = friendlyFireEnabled;
    }

    @NotNull
    public Party getParty() {
        return party;
    }

    public boolean isFriendlyFireEnabled() {
        return friendlyFireEnabled;
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
