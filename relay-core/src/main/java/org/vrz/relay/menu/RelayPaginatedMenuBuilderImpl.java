package org.vrz.relay.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.menu.MenuButton;
import org.vrz.relay.api.menu.PaginatedMenu;
import org.vrz.relay.api.menu.PaginatedMenuBuilder;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class RelayPaginatedMenuBuilderImpl<T> implements PaginatedMenuBuilder<T> {

    private final Plugin plugin;
    private final RelayMenuServiceImpl menuService;

    private Component title = Component.text("Paginated Menu");
    private int rows = 6;
    private final List<T> items = new ArrayList<>();
    private final List<Integer> contentSlots = new ArrayList<>();
    private BiFunction<T, Integer, MenuButton> itemRenderer = null;
    private final Map<Integer, MenuButton> staticButtons = new HashMap<>();

    private int previousButtonSlot = -1;
    private Function<PaginatedMenu<T>, MenuButton> previousButtonSupplier = null;
    private int nextButtonSlot = -1;
    private Function<PaginatedMenu<T>, MenuButton> nextButtonSupplier = null;
    private int pageInfoSlot = -1;
    private BiFunction<Integer, Integer, MenuButton> pageInfoButtonSupplier = null;

    private MenuButton borderButton = null;
    private MenuButton backgroundButton = null;
    private Consumer<org.bukkit.entity.Player> onOpen = p -> {};
    private Consumer<org.bukkit.entity.Player> onClose = p -> {};
    private boolean allowPlayerInventoryClicks = false;
    private long refreshIntervalTicks = 0L;

    public RelayPaginatedMenuBuilderImpl(@NotNull Plugin plugin, @NotNull RelayMenuServiceImpl menuService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.menuService = Objects.requireNonNull(menuService, "menuService cannot be null");
    }

    @Override
    @NotNull
    public PaginatedMenuBuilder<T> title(@NotNull Component title) {
        this.title = Objects.requireNonNull(title, "title cannot be null");
        return this;
    }

    @Override
    @NotNull
    public PaginatedMenuBuilder<T> titleMiniMessage(@NotNull String miniMessageTitle) {
        Objects.requireNonNull(miniMessageTitle, "miniMessageTitle cannot be null");
        this.title = MiniMessage.miniMessage().deserialize(miniMessageTitle);
        return this;
    }

    @Override
    @NotNull
    public PaginatedMenuBuilder<T> rows(int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Chest rows must be between 1 and 6, got " + rows);
        }
        this.rows = rows;
        return this;
    }

    @Override
    @NotNull
    public PaginatedMenuBuilder<T> items(@NotNull Collection<T> items) {
        Objects.requireNonNull(items, "items cannot be null");
        this.items.clear();
        this.items.addAll(items);
        return this;
    }

    @Override
    @NotNull
    public PaginatedMenuBuilder<T> contentSlots(int... slots) {
        Objects.requireNonNull(slots, "slots cannot be null");
        this.contentSlots.clear();
        for (int s : slots) {
            this.contentSlots.add(s);
        }
        return this;
    }

    @Override
    @NotNull
    public PaginatedMenuBuilder<T> contentSlotRange(int startSlot, int endSlot) {
        this.contentSlots.clear();
        for (int s = startSlot; s <= endSlot; s++) {
            this.contentSlots.add(s);
        }
        return this;
    }

    @Override
    @NotNull
    public PaginatedMenuBuilder<T> itemRenderer(@NotNull BiFunction<T, Integer, MenuButton> renderer) {
        this.itemRenderer = Objects.requireNonNull(renderer, "renderer cannot be null");
        return this;
    }

    @Override
    @NotNull
    public PaginatedMenuBuilder<T> staticButton(int slot, @NotNull MenuButton button) {
        Objects.requireNonNull(button, "button cannot be null");
        this.staticButtons.put(slot, button);
        return this;
    }

    @Override
    @NotNull
    public PaginatedMenuBuilder<T> previousPageButton(int slot, @NotNull Function<PaginatedMenu<T>, MenuButton> buttonSupplier) {
        this.previousButtonSlot = slot;
        this.previousButtonSupplier = Objects.requireNonNull(buttonSupplier, "buttonSupplier cannot be null");
        return this;
    }

    @Override
    @NotNull
    public PaginatedMenuBuilder<T> nextPageButton(int slot, @NotNull Function<PaginatedMenu<T>, MenuButton> buttonSupplier) {
        this.nextButtonSlot = slot;
        this.nextButtonSupplier = Objects.requireNonNull(buttonSupplier, "buttonSupplier cannot be null");
        return this;
    }

    @Override
    @NotNull
    public PaginatedMenuBuilder<T> pageInfoButton(int slot, @NotNull BiFunction<Integer, Integer, MenuButton> buttonSupplier) {
        this.pageInfoSlot = slot;
        this.pageInfoButtonSupplier = Objects.requireNonNull(buttonSupplier, "buttonSupplier cannot be null");
        return this;
    }

    @Override
    @NotNull
    public PaginatedMenuBuilder<T> fillBorder(@NotNull MenuButton button) {
        this.borderButton = Objects.requireNonNull(button, "button cannot be null");
        return this;
    }

    @Override
    @NotNull
    public PaginatedMenuBuilder<T> fillBackground(@NotNull MenuButton button) {
        this.backgroundButton = Objects.requireNonNull(button, "button cannot be null");
        return this;
    }

    @Override
    @NotNull
    public PaginatedMenu<T> build() {
        Map<Integer, MenuButton> resolvedStaticButtons = new HashMap<>(staticButtons);

        // Default content slots if none specified: middle rows
        List<Integer> effectiveContentSlots = new ArrayList<>(contentSlots);
        if (effectiveContentSlots.isEmpty()) {
            for (int r = 2; r < rows; r++) {
                for (int c = 2; c <= 8; c++) {
                    effectiveContentSlots.add((r - 1) * 9 + (c - 1));
                }
            }
        }

        // Apply border
        if (borderButton != null) {
            for (int r = 1; r <= rows; r++) {
                for (int c = 1; c <= 9; c++) {
                    if (r == 1 || r == rows || c == 1 || c == 9) {
                        int slot = (r - 1) * 9 + (c - 1);
                        if (!effectiveContentSlots.contains(slot) && !resolvedStaticButtons.containsKey(slot)) {
                            resolvedStaticButtons.put(slot, borderButton);
                        }
                    }
                }
            }
        }

        // Apply background
        if (backgroundButton != null) {
            int totalSize = rows * 9;
            for (int slot = 0; slot < totalSize; slot++) {
                if (!effectiveContentSlots.contains(slot) && !resolvedStaticButtons.containsKey(slot)) {
                    resolvedStaticButtons.put(slot, backgroundButton);
                }
            }
        }

        BiFunction<T, Integer, MenuButton> effectiveRenderer = itemRenderer != null
                ? itemRenderer
                : (it, idx) -> menuService.createButtonBuilder().item(new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE)).build();

        RelayPaginatedMenuImpl<T> menu = new RelayPaginatedMenuImpl<>(
                plugin,
                title,
                rows,
                resolvedStaticButtons,
                onOpen,
                onClose,
                allowPlayerInventoryClicks,
                refreshIntervalTicks,
                menuService,
                items,
                effectiveContentSlots,
                effectiveRenderer,
                previousButtonSlot,
                previousButtonSupplier,
                nextButtonSlot,
                nextButtonSupplier,
                pageInfoSlot,
                pageInfoButtonSupplier
        );

        menuService.registerMenu(menu);
        return menu;
    }
}
