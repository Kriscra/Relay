package org.vrz.relay.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.menu.Menu;

/**
 * Event called when a player closes a Relay virtual menu.
 */
public class MenuCloseEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Menu menu;

    public MenuCloseEvent(@NotNull Player player, @NotNull Menu menu) {
        this.player = player;
        this.menu = menu;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public Menu getMenu() {
        return menu;
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
