package org.vrz.relay.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.menu.Menu;
import org.vrz.relay.api.menu.MenuBuilder;
import org.vrz.relay.api.menu.MenuButton;
import org.vrz.relay.api.menu.MenuClickContext;

import java.util.*;
import java.util.function.Consumer;

public class RelayMenuBuilderImpl implements MenuBuilder {

    private final Plugin plugin;
    private final RelayMenuServiceImpl menuService;

    private Component title = Component.text("Menu");
    private int rows = 3;
    private final Map<Integer, MenuButton> buttons = new HashMap<>();
    private final Map<Character, MenuButton> keyBindings = new HashMap<>();
    private String[] patternMask = null;
    private MenuButton borderButton = null;
    private MenuButton backgroundButton = null;
    private Consumer<Player> onOpen = p -> {};
    private Consumer<Player> onClose = p -> {};
    private boolean allowPlayerInventoryClicks = false;
    private long refreshIntervalTicks = 0L;

    public RelayMenuBuilderImpl(@NotNull Plugin plugin, @NotNull RelayMenuServiceImpl menuService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.menuService = Objects.requireNonNull(menuService, "menuService cannot be null");
    }

    @Override
    @NotNull
    public MenuBuilder title(@NotNull Component title) {
        this.title = Objects.requireNonNull(title, "title cannot be null");
        return this;
    }

    @Override
    @NotNull
    public MenuBuilder titleMiniMessage(@NotNull String miniMessageTitle) {
        Objects.requireNonNull(miniMessageTitle, "miniMessageTitle cannot be null");
        this.title = MiniMessage.miniMessage().deserialize(miniMessageTitle);
        return this;
    }

    @Override
    @NotNull
    public MenuBuilder rows(int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Chest rows must be between 1 and 6, got " + rows);
        }
        this.rows = rows;
        return this;
    }

    @Override
    @NotNull
    public MenuBuilder button(int slot, @NotNull MenuButton button) {
        Objects.requireNonNull(button, "button cannot be null");
        this.buttons.put(slot, button);
        return this;
    }

    @Override
    @NotNull
    public MenuBuilder button(int slot, @NotNull ItemStack item, @NotNull Consumer<MenuClickContext> onClick) {
        Objects.requireNonNull(item, "item cannot be null");
        Objects.requireNonNull(onClick, "onClick cannot be null");
        return button(slot, MenuButton.of(item, onClick));
    }

    @Override
    @NotNull
    public MenuBuilder button(int row, int col, @NotNull MenuButton button) {
        if (row < 1 || row > 6 || col < 1 || col > 9) {
            throw new IllegalArgumentException("Coordinates out of range: row=" + row + ", col=" + col);
        }
        int slot = (row - 1) * 9 + (col - 1);
        return button(slot, button);
    }

    @Override
    @NotNull
    public MenuBuilder pattern(@NotNull String... pattern) {
        Objects.requireNonNull(pattern, "pattern cannot be null");
        this.patternMask = pattern;
        this.rows = pattern.length;
        return this;
    }

    @Override
    @NotNull
    public MenuBuilder bindKey(char key, @NotNull MenuButton button) {
        Objects.requireNonNull(button, "button cannot be null");
        this.keyBindings.put(key, button);
        return this;
    }

    @Override
    @NotNull
    public MenuBuilder fillBorder(@NotNull MenuButton button) {
        this.borderButton = Objects.requireNonNull(button, "button cannot be null");
        return this;
    }

    @Override
    @NotNull
    public MenuBuilder fillBackground(@NotNull MenuButton button) {
        this.backgroundButton = Objects.requireNonNull(button, "button cannot be null");
        return this;
    }

    @Override
    @NotNull
    public MenuBuilder onOpen(@NotNull Consumer<org.bukkit.entity.Player> onOpen) {
        this.onOpen = Objects.requireNonNull(onOpen, "onOpen cannot be null");
        return this;
    }

    @Override
    @NotNull
    public MenuBuilder onClose(@NotNull Consumer<org.bukkit.entity.Player> onClose) {
        this.onClose = Objects.requireNonNull(onClose, "onClose cannot be null");
        return this;
    }

    @Override
    @NotNull
    public MenuBuilder allowPlayerInventoryClicks(boolean allow) {
        this.allowPlayerInventoryClicks = allow;
        return this;
    }

    @Override
    @NotNull
    public MenuBuilder refreshInterval(long periodTicks) {
        this.refreshIntervalTicks = periodTicks;
        return this;
    }

    @Override
    @NotNull
    public Menu build() {
        Map<Integer, MenuButton> resolvedButtons = new HashMap<>(buttons);

        // Apply pattern if configured
        if (patternMask != null) {
            for (int r = 0; r < patternMask.length && r < rows; r++) {
                String rowPattern = patternMask[r];
                for (int c = 0; c < rowPattern.length() && c < 9; c++) {
                    char ch = rowPattern.charAt(c);
                    MenuButton mappedBtn = keyBindings.get(ch);
                    if (mappedBtn != null) {
                        resolvedButtons.put(r * 9 + c, mappedBtn);
                    }
                }
            }
        }

        // Apply border filler
        if (borderButton != null) {
            for (int r = 1; r <= rows; r++) {
                for (int c = 1; c <= 9; c++) {
                    if (r == 1 || r == rows || c == 1 || c == 9) {
                        int slot = (r - 1) * 9 + (c - 1);
                        if (!resolvedButtons.containsKey(slot)) {
                            resolvedButtons.put(slot, borderButton);
                        }
                    }
                }
            }
        }

        // Apply background filler
        if (backgroundButton != null) {
            int totalSize = rows * 9;
            for (int slot = 0; slot < totalSize; slot++) {
                if (!resolvedButtons.containsKey(slot)) {
                    resolvedButtons.put(slot, backgroundButton);
                }
            }
        }

        RelayMenuImpl menu = new RelayMenuImpl(
                plugin,
                title,
                rows,
                resolvedButtons,
                onOpen,
                onClose,
                allowPlayerInventoryClicks,
                refreshIntervalTicks,
                menuService
        );

        menuService.registerMenu(menu);
        return menu;
    }
}
