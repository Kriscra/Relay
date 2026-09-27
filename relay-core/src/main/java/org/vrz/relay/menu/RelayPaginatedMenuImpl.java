package org.vrz.relay.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.menu.MenuButton;
import org.vrz.relay.api.menu.PaginatedMenu;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class RelayPaginatedMenuImpl<T> extends RelayMenuImpl implements PaginatedMenu<T> {

    private final List<T> items = new CopyOnWriteArrayList<>();
    private final List<Integer> contentSlots;
    private final BiFunction<T, Integer, MenuButton> itemRenderer;

    private final int previousButtonSlot;
    private final Function<PaginatedMenu<T>, MenuButton> previousButtonSupplier;
    private final int nextButtonSlot;
    private final Function<PaginatedMenu<T>, MenuButton> nextButtonSupplier;
    private final int pageInfoSlot;
    private final BiFunction<Integer, Integer, MenuButton> pageInfoButtonSupplier;

    private final Map<UUID, Integer> playerPages = new ConcurrentHashMap<>();

    public RelayPaginatedMenuImpl(
            @NotNull Plugin plugin,
            @NotNull Component title,
            int rows,
            @NotNull Map<Integer, MenuButton> staticButtons,
            @NotNull Consumer<Player> onOpen,
            @NotNull Consumer<Player> onClose,
            boolean allowPlayerInventoryClicks,
            long refreshIntervalTicks,
            @NotNull RelayMenuServiceImpl menuService,
            @NotNull Collection<T> initialItems,
            @NotNull List<Integer> contentSlots,
            @NotNull BiFunction<T, Integer, MenuButton> itemRenderer,
            int previousButtonSlot,
            Function<PaginatedMenu<T>, MenuButton> previousButtonSupplier,
            int nextButtonSlot,
            Function<PaginatedMenu<T>, MenuButton> nextButtonSupplier,
            int pageInfoSlot,
            BiFunction<Integer, Integer, MenuButton> pageInfoButtonSupplier
    ) {
        super(plugin, title, rows, staticButtons, onOpen, onClose, allowPlayerInventoryClicks, refreshIntervalTicks, menuService);
        this.items.addAll(initialItems);
        this.contentSlots = Collections.unmodifiableList(new ArrayList<>(contentSlots));
        this.itemRenderer = Objects.requireNonNull(itemRenderer, "itemRenderer cannot be null");
        this.previousButtonSlot = previousButtonSlot;
        this.previousButtonSupplier = previousButtonSupplier;
        this.nextButtonSlot = nextButtonSlot;
        this.nextButtonSupplier = nextButtonSupplier;
        this.pageInfoSlot = pageInfoSlot;
        this.pageInfoButtonSupplier = pageInfoButtonSupplier;
    }

    @Override
    public int getCurrentPage(@NotNull Player player) {
        return playerPages.getOrDefault(player.getUniqueId(), 0);
    }

    @Override
    public int getMaxPages() {
        if (contentSlots.isEmpty() || items.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil((double) items.size() / contentSlots.size());
    }

    @Override
    public void setPage(@NotNull Player player, int page) {
        int max = Math.max(0, getMaxPages() - 1);
        int clamped = Math.max(0, Math.min(page, max));
        playerPages.put(player.getUniqueId(), clamped);
        renderPage(player);
        updateAllSlots(player);
    }

    @Override
    public void nextPage(@NotNull Player player) {
        int current = getCurrentPage(player);
        if (current < getMaxPages() - 1) {
            setPage(player, current + 1);
        }
    }

    @Override
    public void previousPage(@NotNull Player player) {
        int current = getCurrentPage(player);
        if (current > 0) {
            setPage(player, current - 1);
        }
    }

    @Override
    @NotNull
    public List<T> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public void setItems(@NotNull List<T> newItems) {
        Objects.requireNonNull(newItems, "newItems cannot be null");
        this.items.clear();
        this.items.addAll(newItems);
        refreshViewers();
    }

    @Override
    public void addItem(@NotNull T item) {
        Objects.requireNonNull(item, "item cannot be null");
        this.items.add(item);
        refreshViewers();
    }

    @Override
    public void removeItem(@NotNull T item) {
        Objects.requireNonNull(item, "item cannot be null");
        this.items.remove(item);
        refreshViewers();
    }

    @Override
    public void open(@NotNull Player player) {
        playerPages.putIfAbsent(player.getUniqueId(), 0);
        renderPage(player);
        super.open(player);
    }

    @Override
    public void refreshViewers() {
        for (UUID viewerId : getViewers()) {
            Player p = org.bukkit.Bukkit.getPlayer(viewerId);
            if (p != null && p.isOnline()) {
                renderPage(p);
                updateAllSlots(p);
            }
        }
    }

    private void renderPage(@NotNull Player player) {
        int page = getCurrentPage(player);
        int pageSize = contentSlots.size();
        if (pageSize == 0) return;

        int startIndex = page * pageSize;

        for (int i = 0; i < pageSize; i++) {
            int slot = contentSlots.get(i);
            int itemIndex = startIndex + i;
            if (itemIndex < items.size()) {
                T item = items.get(itemIndex);
                MenuButton btn = itemRenderer.apply(item, itemIndex);
                setButton(slot, btn);
            } else {
                setButton(slot, null);
            }
        }

        // Navigation controls
        if (previousButtonSlot >= 0 && previousButtonSupplier != null) {
            setButton(previousButtonSlot, previousButtonSupplier.apply(this));
        }
        if (nextButtonSlot >= 0 && nextButtonSupplier != null) {
            setButton(nextButtonSlot, nextButtonSupplier.apply(this));
        }
        if (pageInfoSlot >= 0 && pageInfoButtonSupplier != null) {
            setButton(pageInfoSlot, pageInfoButtonSupplier.apply(page + 1, getMaxPages()));
        }
    }
}
