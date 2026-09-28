package org.vrz.relay.notification;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.notification.ActionBarBuilder;
import org.vrz.relay.api.notification.ActionBarNotification;
import org.vrz.relay.api.notification.NotificationPriority;

import java.time.Duration;
import java.util.Objects;

public final class RelayActionBarBuilderImpl implements ActionBarBuilder {

    private final RelayNotificationServiceImpl service;
    private final Player player;

    private Component message = Component.empty();
    private NotificationPriority priority = NotificationPriority.NORMAL;
    private Duration duration = Duration.ofSeconds(3);
    private Sound sound;
    private Runnable onComplete;

    public RelayActionBarBuilderImpl(@NotNull RelayNotificationServiceImpl service, @NotNull Player player) {
        this.service = Objects.requireNonNull(service, "service cannot be null");
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    @Override
    @NotNull
    public ActionBarBuilder message(@NotNull Component message) {
        this.message = Objects.requireNonNull(message, "message cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ActionBarBuilder messageMiniMessage(@NotNull String miniMessage) {
        this.message = MiniMessage.miniMessage().deserialize(Objects.requireNonNull(miniMessage, "miniMessage cannot be null"));
        return this;
    }

    @Override
    @NotNull
    public ActionBarBuilder priority(@NotNull NotificationPriority priority) {
        this.priority = Objects.requireNonNull(priority, "priority cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ActionBarBuilder duration(@NotNull Duration duration) {
        this.duration = Objects.requireNonNull(duration, "duration cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ActionBarBuilder sound(@NotNull Sound sound) {
        this.sound = Objects.requireNonNull(sound, "sound cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ActionBarBuilder sound(@NotNull org.bukkit.Sound sound, float volume, float pitch) {
        Objects.requireNonNull(sound, "sound cannot be null");
        this.sound = Sound.sound(sound.key(), Sound.Source.PLAYER, volume, pitch);
        return this;
    }

    @Override
    @NotNull
    public ActionBarBuilder onComplete(@NotNull Runnable onComplete) {
        this.onComplete = Objects.requireNonNull(onComplete, "onComplete cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ActionBarNotification send(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        RelayActionBarNotificationImpl notification = new RelayActionBarNotificationImpl(
                plugin,
                player,
                message,
                priority,
                duration,
                sound,
                onComplete
        );
        service.enqueueActionBar(notification);
        return notification;
    }
}
