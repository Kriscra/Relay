package org.vrz.relay.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.menu.Menu;
import org.vrz.relay.api.menu.MenuClickContext;

import java.util.Objects;

public class RelayMenuClickContextImpl implements MenuClickContext {

    private final Player player;
    private final ClickType clickType;
    private final int slot;
    private final ItemStack clickedItem;
    private final Menu menu;
    private boolean cancelled = true;

    public RelayMenuClickContextImpl(
            @NotNull Player player,
            @NotNull ClickType clickType,
            int slot,
            @Nullable ItemStack clickedItem,
            @NotNull Menu menu
    ) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.clickType = Objects.requireNonNull(clickType, "clickType cannot be null");
        this.slot = slot;
        this.clickedItem = clickedItem;
        this.menu = Objects.requireNonNull(menu, "menu cannot be null");
    }

    @Override
    @NotNull
    public Player getPlayer() {
        return player;
    }

    @Override
    @NotNull
    public ClickType getClickType() {
        return clickType;
    }

    @Override
    public int getSlot() {
        return slot;
    }

    @Override
    @Nullable
    public ItemStack getClickedItem() {
        return clickedItem != null ? clickedItem.clone() : null;
    }

    @Override
    @NotNull
    public Menu getMenu() {
        return menu;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public void close() {
        menu.close(player);
    }

    @Override
    public void refresh() {
        menu.updateAllSlots(player);
    }

    @Override
    public void open(@NotNull Menu otherMenu) {
        Objects.requireNonNull(otherMenu, "otherMenu cannot be null");
        otherMenu.open(player);
    }
}
