package org.vrz.relay.menu;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.menu.MenuButton;
import org.vrz.relay.api.menu.MenuButtonBuilder;
import org.vrz.relay.api.menu.MenuClickContext;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class RelayMenuButtonBuilderImpl implements MenuButtonBuilder {

    private Function<Player, ItemStack> itemSupplier = p -> null;
    private Consumer<MenuClickContext> clickHandler = ctx -> {};
    private Sound sound;
    private float volume = 1.0f;
    private float pitch = 1.0f;
    private String requiredPermission;
    private ItemStack noPermissionItem;
    private long debounceMillis = 150L; // Safe default debounce
    private Predicate<Player> condition = p -> true;

    @Override
    @NotNull
    public MenuButtonBuilder item(@NotNull ItemStack item) {
        Objects.requireNonNull(item, "item cannot be null");
        ItemStack clone = item.clone();
        this.itemSupplier = p -> clone.clone();
        return this;
    }

    @Override
    @NotNull
    public MenuButtonBuilder itemSupplier(@NotNull Function<Player, ItemStack> itemSupplier) {
        this.itemSupplier = Objects.requireNonNull(itemSupplier, "itemSupplier cannot be null");
        return this;
    }

    @Override
    @NotNull
    public MenuButtonBuilder onClick(@NotNull Consumer<MenuClickContext> onClick) {
        this.clickHandler = Objects.requireNonNull(onClick, "onClick cannot be null");
        return this;
    }

    @Override
    @NotNull
    public MenuButtonBuilder onClick(@NotNull Runnable onClick) {
        Objects.requireNonNull(onClick, "onClick cannot be null");
        this.clickHandler = ctx -> onClick.run();
        return this;
    }

    @Override
    @NotNull
    public MenuButtonBuilder sound(@NotNull Sound sound) {
        this.sound = Objects.requireNonNull(sound, "sound cannot be null");
        return this;
    }

    @Override
    @NotNull
    public MenuButtonBuilder sound(@NotNull Sound sound, float volume, float pitch) {
        this.sound = Objects.requireNonNull(sound, "sound cannot be null");
        this.volume = volume;
        this.pitch = pitch;
        return this;
    }

    @Override
    @NotNull
    public MenuButtonBuilder requirePermission(@NotNull String permission) {
        this.requiredPermission = Objects.requireNonNull(permission, "permission cannot be null");
        return this;
    }

    @Override
    @NotNull
    public MenuButtonBuilder requirePermission(@NotNull String permission, @Nullable ItemStack noPermItem) {
        this.requiredPermission = Objects.requireNonNull(permission, "permission cannot be null");
        this.noPermissionItem = noPermItem != null ? noPermItem.clone() : null;
        return this;
    }

    @Override
    @NotNull
    public MenuButtonBuilder debounce(long millis) {
        this.debounceMillis = millis;
        return this;
    }

    @Override
    @NotNull
    public MenuButtonBuilder condition(@NotNull Predicate<Player> condition) {
        this.condition = Objects.requireNonNull(condition, "condition cannot be null");
        return this;
    }

    @Override
    @NotNull
    public MenuButton build() {
        return new RelayMenuButtonImpl(
                itemSupplier,
                clickHandler,
                sound,
                volume,
                pitch,
                requiredPermission,
                noPermissionItem,
                debounceMillis,
                condition
        );
    }
}
