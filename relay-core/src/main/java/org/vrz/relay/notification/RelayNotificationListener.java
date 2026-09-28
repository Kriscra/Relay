package org.vrz.relay.notification;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Ensures zero-leak lifecycle cleanup when players leave the server.
 */
public final class RelayNotificationListener implements Listener {

    private final RelayNotificationServiceImpl service;

    public RelayNotificationListener(@NotNull RelayNotificationServiceImpl service) {
        this.service = Objects.requireNonNull(service, "service cannot be null");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        service.handlePlayerQuit(event.getPlayer());
    }
}
