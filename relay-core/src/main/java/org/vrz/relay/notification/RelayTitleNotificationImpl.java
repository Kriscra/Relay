package org.vrz.relay.notification;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.notification.NotificationPriority;
import org.vrz.relay.api.notification.TitleNotification;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RelayTitleNotificationImpl implements TitleNotification, Comparable<RelayTitleNotificationImpl> {

    private final Plugin plugin;
    private final Player player;
    private final Component title;
    private final Component subtitle;
    private final NotificationPriority priority;
    private final Duration fadeIn;
    private final Duration stay;
    private final Duration fadeOut;
    private final Sound sound;
    private final Runnable onComplete;
    private final long createdAtMillis;
    private final long totalDurationMillis;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private volatile long startedDisplayMillis = -1;

    public RelayTitleNotificationImpl(@NotNull Plugin plugin,
                                      @NotNull Player player,
                                      @NotNull Component title,
                                      @NotNull Component subtitle,
                                      @NotNull NotificationPriority priority,
                                      @NotNull Duration fadeIn,
                                      @NotNull Duration stay,
                                      @NotNull Duration fadeOut,
                                      @Nullable Sound sound,
                                      @Nullable Runnable onComplete) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.subtitle = Objects.requireNonNull(subtitle, "subtitle cannot be null");
        this.priority = Objects.requireNonNull(priority, "priority cannot be null");
        this.fadeIn = Objects.requireNonNull(fadeIn, "fadeIn cannot be null");
        this.stay = Objects.requireNonNull(stay, "stay cannot be null");
        this.fadeOut = Objects.requireNonNull(fadeOut, "fadeOut cannot be null");
        this.sound = sound;
        this.onComplete = onComplete;
        this.createdAtMillis = System.currentTimeMillis();
        this.totalDurationMillis = Math.max(500, fadeIn.toMillis() + stay.toMillis() + fadeOut.toMillis());
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
    public Component getSubtitle() {
        return subtitle;
    }

    @Override
    @NotNull
    public NotificationPriority getPriority() {
        return priority;
    }

    @Override
    @NotNull
    public Duration getFadeIn() {
        return fadeIn;
    }

    @Override
    @NotNull
    public Duration getStay() {
        return stay;
    }

    @Override
    @NotNull
    public Duration getFadeOut() {
        return fadeOut;
    }

    @Override
    @Nullable
    public Sound getSound() {
        return sound;
    }

    public void markStarted() {
        if (this.startedDisplayMillis < 0) {
            this.startedDisplayMillis = System.currentTimeMillis();
        }
    }

    @Override
    public boolean isExpired() {
        if (cancelled.get()) {
            return true;
        }
        if (startedDisplayMillis < 0) {
            return false;
        }
        return (System.currentTimeMillis() - startedDisplayMillis) >= totalDurationMillis;
    }

    @Override
    public void cancel() {
        cancelled.set(true);
    }

    public void triggerComplete() {
        if (onComplete != null) {
            try {
                onComplete.run();
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public int compareTo(@NotNull RelayTitleNotificationImpl o) {
        int pCompare = Integer.compare(o.priority.getLevel(), this.priority.getLevel());
        if (pCompare != 0) {
            return pCompare;
        }
        return Long.compare(this.createdAtMillis, o.createdAtMillis);
    }
}
