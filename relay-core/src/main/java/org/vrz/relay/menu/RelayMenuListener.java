package org.vrz.relay.menu;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.menu.Menu;

import java.util.Objects;
import java.util.Optional;

public class RelayMenuListener implements Listener {

    private final RelayMenuServiceImpl menuService;

    public RelayMenuListener(@NotNull RelayMenuServiceImpl menuService) {
        this.menuService = Objects.requireNonNull(menuService, "menuService cannot be null");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity who = event.getWhoClicked();
        if (!(who instanceof Player player)) {
            return;
        }

        Optional<Menu> openMenu = menuService.getOpenMenu(player);
        if (openMenu.isPresent() && openMenu.get() instanceof RelayMenuImpl menuImpl) {
            menuImpl.handleClickInternal(event, player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        HumanEntity who = event.getWhoClicked();
        if (!(who instanceof Player player)) {
            return;
        }

        Optional<Menu> openMenu = menuService.getOpenMenu(player);
        if (openMenu.isPresent()) {
            Menu menu = openMenu.get();
            int topSize = menu.getSize();
            // If dragging across top menu slots, cancel immediately to prevent item tampering
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot >= 0 && rawSlot < topSize) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        HumanEntity who = event.getPlayer();
        if (!(who instanceof Player player)) {
            return;
        }

        Optional<Menu> openMenu = menuService.getOpenMenu(player);
        if (openMenu.isPresent() && openMenu.get() instanceof RelayMenuImpl menuImpl) {
            menuImpl.handleCloseInternal(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Optional<Menu> openMenu = menuService.getOpenMenu(player);
        if (openMenu.isPresent() && openMenu.get() instanceof RelayMenuImpl menuImpl) {
            menuImpl.handleCloseInternal(player);
        }
    }
}
