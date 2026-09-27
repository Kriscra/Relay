package org.vrz.relay.menu;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.menu.MenuButton;
import org.vrz.relay.api.menu.MenuClickContext;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class RelayMenuButtonImpl implements MenuButton {

    private final Function<Player, ItemStack> itemSupplier;
    private final Consumer<MenuClickContext> clickHandler;
    private final Sound clickSound;
    private final float volume;
    private final float pitch;
    private final String requiredPermission;
    private final ItemStack noPermissionItem;
    private final long debounceMillis;
    private final Predicate<Player> condition;
    private final Map<UUID, Long> lastClickMap = new ConcurrentHashMap<>();

    public RelayMenuButtonImpl(
            @NotNull Function<Player, ItemStack> itemSupplier,
            @NotNull Consumer<MenuClickContext> clickHandler,
            @Nullable Sound clickSound,
            float volume,
            float pitch,
            @Nullable String requiredPermission,
            @Nullable ItemStack noPermissionItem,
            long debounceMillis,
            @NotNull Predicate<Player> condition
    ) {
        this.itemSupplier = Objects.requireNonNull(itemSupplier, "itemSupplier cannot be null");
        this.clickHandler = Objects.requireNonNull(clickHandler, "clickHandler cannot be null");
        this.clickSound = clickSound;
        this.volume = volume;
        this.pitch = pitch;
        this.requiredPermission = requiredPermission;
        this.noPermissionItem = noPermissionItem != null ? noPermissionItem.clone() : null;
        this.debounceMillis = Math.max(0, debounceMillis);
        this.condition = Objects.requireNonNull(condition, "condition cannot be null");
    }

    @Override
    @Nullable
    public ItemStack getItem(@NotNull Player player) {
        if (requiredPermission != null && !player.hasPermission(requiredPermission)) {
            if (noPermissionItem != null) {
                return noPermissionItem.clone();
            }
        }
        ItemStack item = itemSupplier.apply(player);
        return item != null ? item.clone() : null;
    }

    @Override
    @NotNull
    public Consumer<MenuClickContext> getClickHandler() {
        return clickHandler;
    }

    @Override
    @Nullable
    public Sound getClickSound() {
        return clickSound;
    }

    @Override
    public float getSoundVolume() {
        return volume;
    }

    @Override
    public float getSoundPitch() {
        return pitch;
    }

    @Override
    @Nullable
    public String getRequiredPermission() {
        return requiredPermission;
    }

    @Override
    @Nullable
    public ItemStack getNoPermissionItem() {
        return noPermissionItem != null ? noPermissionItem.clone() : null;
    }

    @Override
    public long getDebounceMillis() {
        return debounceMillis;
    }

    @Override
    public boolean canClick(@NotNull Player player) {
        // 1. Permission check
        if (requiredPermission != null && !player.hasPermission(requiredPermission)) {
            return false;
        }

        // 2. Custom condition
        if (!condition.test(player)) {
            return false;
        }

        // 3. Debounce check
        if (debounceMillis > 0) {
            long now = System.currentTimeMillis();
            Long last = lastClickMap.get(player.getUniqueId());
            if (last != null && (now - last) < debounceMillis) {
                return false;
            }
            lastClickMap.put(player.getUniqueId(), now);
        }

        return true;
    }
}
