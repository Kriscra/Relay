package org.vrz.relay.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.event.MenuCloseEvent;
import org.vrz.relay.api.event.MenuOpenEvent;
import org.vrz.relay.api.menu.Menu;
import org.vrz.relay.api.menu.MenuButton;
import org.vrz.relay.api.menu.MenuClickContext;
import org.vrz.relay.util.RelaySchedulerBridge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RelayMenuImpl implements Menu {

    private final UUID id = UUID.randomUUID();
    private final Plugin plugin;
    private final Component title;
    private final int rows;
    private final int size;
    private final Map<Integer, MenuButton> buttons = new ConcurrentHashMap<>();
    private final Consumer<Player> onOpen;
    private final Consumer<Player> onClose;
    private final boolean allowPlayerInventoryClicks;
    private final long refreshIntervalTicks;

    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Inventory> viewerInventories = new ConcurrentHashMap<>();
    private final RelayMenuServiceImpl menuService;

    private volatile Runnable cancelTimerTask = null;

    public RelayMenuImpl(
            @NotNull Plugin plugin,
            @NotNull Component title,
            int rows,
            @NotNull Map<Integer, MenuButton> initialButtons,
            @NotNull Consumer<Player> onOpen,
            @NotNull Consumer<Player> onClose,
            boolean allowPlayerInventoryClicks,
            long refreshIntervalTicks,
            @NotNull RelayMenuServiceImpl menuService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.title = Objects.requireNonNull(title, "title cannot be null");
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Chest rows must be between 1 and 6, got " + rows);
        }
        this.rows = rows;
        this.size = rows * 9;
        this.buttons.putAll(initialButtons);
        this.onOpen = Objects.requireNonNull(onOpen, "onOpen cannot be null");
        this.onClose = Objects.requireNonNull(onClose, "onClose cannot be null");
        this.allowPlayerInventoryClicks = allowPlayerInventoryClicks;
        this.refreshIntervalTicks = refreshIntervalTicks;
        this.menuService = Objects.requireNonNull(menuService, "menuService cannot be null");
    }

    @Override
    @NotNull
    public UUID getId() {
        return id;
    }

    @Override
    @NotNull
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    @NotNull
    public Component getTitle() {
        return title;
    }

    @Override
    public int getRows() {
        return rows;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    @NotNull
    public Optional<MenuButton> getButton(int slot) {
        return Optional.ofNullable(buttons.get(slot));
    }

    @Override
    public void setButton(int slot, @Nullable MenuButton button) {
        if (slot < 0 || slot >= size) {
            return;
        }
        if (button == null) {
            buttons.remove(slot);
        } else {
            buttons.put(slot, button);
        }
        // Update slot for currently active viewers
        for (UUID viewerId : viewers) {
            Player p = Bukkit.getPlayer(viewerId);
            if (p != null && p.isOnline()) {
                updateSlot(p, slot);
            }
        }
    }

    @Override
    public void setButton(int row, int col, @Nullable MenuButton button) {
        if (row < 1 || row > rows || col < 1 || col > 9) {
            return;
        }
        int slot = (row - 1) * 9 + (col - 1);
        setButton(slot, button);
    }

    @Override
    public void removeButton(int slot) {
        setButton(slot, null);
    }

    @Override
    public void fillBorder(@NotNull MenuButton button) {
        Objects.requireNonNull(button, "button cannot be null");
        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= 9; c++) {
                if (r == 1 || r == rows || c == 1 || c == 9) {
                    setButton(r, c, button);
                }
            }
        }
    }

    @Override
    public void fillBackground(@NotNull MenuButton button) {
        Objects.requireNonNull(button, "button cannot be null");
        for (int slot = 0; slot < size; slot++) {
            if (!buttons.containsKey(slot)) {
                setButton(slot, button);
            }
        }
    }

    @Override
    public void fillRectangle(int fromRow, int fromCol, int toRow, int toCol, @NotNull MenuButton button) {
        Objects.requireNonNull(button, "button cannot be null");
        int minR = Math.max(1, Math.min(fromRow, toRow));
        int maxR = Math.min(rows, Math.max(fromRow, toRow));
        int minC = Math.max(1, Math.min(fromCol, toCol));
        int maxC = Math.min(9, Math.max(fromCol, toCol));

        for (int r = minR; r <= maxR; r++) {
            for (int c = minC; c <= maxC; c++) {
                setButton(r, c, button);
            }
        }
    }

    @Override
    public void updateSlot(@NotNull Player player, int slot) {
        Inventory inv = viewerInventories.get(player.getUniqueId());
        if (inv == null || slot < 0 || slot >= size) {
            return;
        }
        MenuButton btn = buttons.get(slot);
        ItemStack item = btn != null ? btn.getItem(player) : null;
        RelaySchedulerBridge.runOnPlayer(plugin, player, () -> {
            inv.setItem(slot, item);
        });
    }

    @Override
    public void updateAllSlots(@NotNull Player player) {
        Inventory inv = viewerInventories.get(player.getUniqueId());
        if (inv == null) {
            return;
        }
        RelaySchedulerBridge.runOnPlayer(plugin, player, () -> {
            for (int slot = 0; slot < size; slot++) {
                MenuButton btn = buttons.get(slot);
                inv.setItem(slot, btn != null ? btn.getItem(player) : null);
            }
        });
    }

    @Override
    public void open(@NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");

        // Fire MenuOpenEvent
        MenuOpenEvent event = new MenuOpenEvent(player, this);
        if (Bukkit.getServer() != null) {
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
        }

        RelaySchedulerBridge.runOnPlayer(plugin, player, () -> {
            Inventory inv;
            if (Bukkit.getServer() != null) {
                inv = Bukkit.createInventory(player, size, title);
            } else {
                // Mock test fallback
                inv = null;
            }

            if (inv != null) {
                for (int slot = 0; slot < size; slot++) {
                    MenuButton btn = buttons.get(slot);
                    if (btn != null) {
                        inv.setItem(slot, btn.getItem(player));
                    }
                }
                player.openInventory(inv);
                viewerInventories.put(player.getUniqueId(), inv);
            }

            viewers.add(player.getUniqueId());
            menuService.registerSession(player.getUniqueId(), this);
            onOpen.accept(player);

            startRefreshTimerIfNeeded();
        });
    }

    @Override
    public void close(@NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        RelaySchedulerBridge.runOnPlayer(plugin, player, player::closeInventory);
    }

    /**
     * Called when the inventory close event fires for this player.
     */
    void handleCloseInternal(@NotNull Player player) {
        if (viewers.remove(player.getUniqueId())) {
            viewerInventories.remove(player.getUniqueId());
            menuService.unregisterSession(player.getUniqueId());

            if (Bukkit.getServer() != null) {
                Bukkit.getPluginManager().callEvent(new MenuCloseEvent(player, this));
            }
            onClose.accept(player);

            if (viewers.isEmpty()) {
                stopRefreshTimer();
            }
        }
    }

    /**
     * Handles inventory click events dispatched by RelayMenuListener.
     */
    void handleClickInternal(@NotNull InventoryClickEvent event, @NotNull Player player) {
        int rawSlot = event.getRawSlot();
        int slot = event.getSlot();

        // 1. Clicked inside the menu top inventory
        if (rawSlot >= 0 && rawSlot < size) {
            event.setCancelled(true); // Always cancel top menu clicks by default

            MenuButton button = buttons.get(slot);
            if (button == null) {
                return;
            }

            if (!button.canClick(player)) {
                return;
            }

            Sound sound = button.getClickSound();
            if (sound != null) {
                player.playSound(player.getLocation(), sound, button.getSoundVolume(), button.getSoundPitch());
            }

            MenuClickContext context = new RelayMenuClickContextImpl(
                    player,
                    event.getClick(),
                    slot,
                    event.getCurrentItem(),
                    this
            );

            try {
                button.getClickHandler().accept(context);
            } catch (Throwable t) {
                plugin.getLogger().warning("Error executing menu button click handler: " + t.getMessage());
            }

            if (context.isCancelled()) {
                event.setCancelled(true);
            }
            return;
        }

        // 2. Clicked inside player inventory (bottom)
        if (!allowPlayerInventoryClicks) {
            event.setCancelled(true);
        } else if (event.isShiftClick()) {
            // Shift clicking items into the menu is always blocked to prevent item loss/corruption
            event.setCancelled(true);
        }
    }

    @Override
    @NotNull
    public Set<UUID> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }

    @Override
    public boolean isViewer(@NotNull Player player) {
        return viewers.contains(player.getUniqueId());
    }

    @Override
    public void refreshViewers() {
        for (UUID viewerId : viewers) {
            Player p = Bukkit.getPlayer(viewerId);
            if (p != null && p.isOnline()) {
                updateAllSlots(p);
            }
        }
    }

    @Override
    public void closeAll() {
        for (UUID viewerId : new HashSet<>(viewers)) {
            Player p = Bukkit.getPlayer(viewerId);
            if (p != null && p.isOnline()) {
                close(p);
            }
        }
        stopRefreshTimer();
    }

    @Override
    @NotNull
    public Optional<Inventory> getInventory(@NotNull Player player) {
        return Optional.ofNullable(viewerInventories.get(player.getUniqueId()));
    }

    private synchronized void startRefreshTimerIfNeeded() {
        if (refreshIntervalTicks <= 0 || cancelTimerTask != null) {
            return;
        }
        cancelTimerTask = RelaySchedulerBridge.runTimer(
                plugin,
                this::refreshViewers,
                refreshIntervalTicks,
                refreshIntervalTicks
        );
    }

    private synchronized void stopRefreshTimer() {
        if (cancelTimerTask != null) {
            cancelTimerTask.run();
            cancelTimerTask = null;
        }
    }
}
