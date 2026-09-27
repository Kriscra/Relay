package org.vrz.relay.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.menu.Menu;

/**
 * Event called when a player opens a Relay virtual menu.
 * Can be cancelled to prevent the menu from opening.
 */
public class MenuOpenEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Menu menu;
    private boolean cancelled;

    public MenuOpenEvent(@NotNull Player player, @NotNull Menu menu) {
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

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
