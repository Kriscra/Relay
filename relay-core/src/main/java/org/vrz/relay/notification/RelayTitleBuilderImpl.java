package org.vrz.relay.notification;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.notification.NotificationPriority;
import org.vrz.relay.api.notification.TitleBuilder;
import org.vrz.relay.api.notification.TitleNotification;

import java.time.Duration;
import java.util.Objects;

public final class RelayTitleBuilderImpl implements TitleBuilder {

    private final RelayNotificationServiceImpl service;
    private final Player player;

    private Component title = Component.empty();
    private Component subtitle = Component.empty();
    private NotificationPriority priority = NotificationPriority.NORMAL;
    private Duration fadeIn = Duration.ofMillis(500);
    private Duration stay = Duration.ofSeconds(3);
    private Duration fadeOut = Duration.ofMillis(500);
    private Sound sound;
    private Runnable onComplete;

    public RelayTitleBuilderImpl(@NotNull RelayNotificationServiceImpl service, @NotNull Player player) {
        this.service = Objects.requireNonNull(service, "service cannot be null");
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    @Override
    @NotNull
    public TitleBuilder title(@NotNull Component title) {
        this.title = Objects.requireNonNull(title, "title cannot be null");
        return this;
    }

    @Override
    @NotNull
    public TitleBuilder titleMiniMessage(@NotNull String miniMessage) {
        this.title = MiniMessage.miniMessage().deserialize(Objects.requireNonNull(miniMessage, "miniMessage cannot be null"));
        return this;
    }

    @Override
    @NotNull
    public TitleBuilder subtitle(@NotNull Component subtitle) {
        this.subtitle = Objects.requireNonNull(subtitle, "subtitle cannot be null");
        return this;
    }

    @Override
    @NotNull
    public TitleBuilder subtitleMiniMessage(@NotNull String miniMessage) {
        this.subtitle = MiniMessage.miniMessage().deserialize(Objects.requireNonNull(miniMessage, "miniMessage cannot be null"));
        return this;
    }

    @Override
    @NotNull
    public TitleBuilder priority(@NotNull NotificationPriority priority) {
        this.priority = Objects.requireNonNull(priority, "priority cannot be null");
        return this;
    }

    @Override
    @NotNull
    public TitleBuilder times(@NotNull Duration fadeIn, @NotNull Duration stay, @NotNull Duration fadeOut) {
        this.fadeIn = Objects.requireNonNull(fadeIn, "fadeIn cannot be null");
        this.stay = Objects.requireNonNull(stay, "stay cannot be null");
        this.fadeOut = Objects.requireNonNull(fadeOut, "fadeOut cannot be null");
        return this;
    }

    @Override
    @NotNull
    public TitleBuilder sound(@NotNull Sound sound) {
        this.sound = Objects.requireNonNull(sound, "sound cannot be null");
        return this;
    }

    @Override
    @NotNull
    public TitleBuilder sound(@NotNull org.bukkit.Sound sound, float volume, float pitch) {
        Objects.requireNonNull(sound, "sound cannot be null");
        this.sound = Sound.sound(sound.key(), Sound.Source.PLAYER, volume, pitch);
        return this;
    }

    @Override
    @NotNull
    public TitleBuilder onComplete(@NotNull Runnable onComplete) {
        this.onComplete = Objects.requireNonNull(onComplete, "onComplete cannot be null");
        return this;
    }

    @Override
    @NotNull
    public TitleNotification send(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        RelayTitleNotificationImpl notification = new RelayTitleNotificationImpl(
                plugin,
                player,
                title,
                subtitle,
                priority,
                fadeIn,
                stay,
                fadeOut,
                sound,
                onComplete
        );
        service.enqueueTitle(notification);
        return notification;
    }
}
