package org.vrz.relay.notification;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.notification.ToastFrame;
import org.vrz.relay.api.notification.ToastNotification;

import java.util.Objects;

public final class RelayToastNotificationImpl implements ToastNotification {

    private final Plugin plugin;
    private final Player player;
    private final Component title;
    private final Component description;
    private final Material icon;
    private final ToastFrame frame;
    private final Sound sound;

    public RelayToastNotificationImpl(@NotNull Plugin plugin,
                                      @NotNull Player player,
                                      @NotNull Component title,
                                      @NotNull Component description,
                                      @NotNull Material icon,
                                      @NotNull ToastFrame frame,
                                      @Nullable Sound sound) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.icon = Objects.requireNonNull(icon, "icon cannot be null");
        this.frame = Objects.requireNonNull(frame, "frame cannot be null");
        this.sound = sound;
    }

    @Override
    @NotNull
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    @NotNull
    public Player getPlayer() {
        return player;
    }

    @Override
    @NotNull
    public Component getTitle() {
        return title;
    }

    @Override
    @NotNull
    public Component getDescription() {
        return description;
    }

    @Override
    @NotNull
    public Material getIcon() {
        return icon;
    }

    @Override
    @NotNull
    public ToastFrame getFrame() {
        return frame;
    }

    @Override
    @Nullable
    public Sound getSound() {
        return sound;
    }
}
