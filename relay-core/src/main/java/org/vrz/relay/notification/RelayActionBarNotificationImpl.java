package org.vrz.relay.notification;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.notification.ActionBarNotification;
import org.vrz.relay.api.notification.NotificationPriority;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RelayActionBarNotificationImpl implements ActionBarNotification, Comparable<RelayActionBarNotificationImpl> {

    private final Plugin plugin;
    private final Player player;
    private final Component message;
    private final NotificationPriority priority;
    private final Duration duration;
    private final Sound sound;
    private final Runnable onComplete;
    private final long createdAtMillis;
    private final long durationMillis;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private volatile long startedDisplayMillis = -1;

    public RelayActionBarNotificationImpl(@NotNull Plugin plugin,
                                          @NotNull Player player,
                                          @NotNull Component message,
                                          @NotNull NotificationPriority priority,
                                          @NotNull Duration duration,
                                          @Nullable Sound sound,
                                          @Nullable Runnable onComplete) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.message = Objects.requireNonNull(message, "message cannot be null");
        this.priority = Objects.requireNonNull(priority, "priority cannot be null");
        this.duration = Objects.requireNonNull(duration, "duration cannot be null");
        this.sound = sound;
        this.onComplete = onComplete;
        this.createdAtMillis = System.currentTimeMillis();
        this.durationMillis = Math.max(500, duration.toMillis());
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
    public Component getMessage() {
        return message;
    }

    @Override
    @NotNull
    public NotificationPriority getPriority() {
        return priority;
    }

    @Override
    @NotNull
    public Duration getDuration() {
        return duration;
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

    public boolean hasStarted() {
        return this.startedDisplayMillis > 0;
    }

    @Override
    public long getRemainingMillis() {
        if (cancelled.get()) {
            return 0;
        }
        if (startedDisplayMillis < 0) {
            return durationMillis;
        }
        long elapsed = System.currentTimeMillis() - startedDisplayMillis;
        return Math.max(0, durationMillis - elapsed);
    }

    @Override
    public boolean isExpired() {
        if (cancelled.get()) {
            return true;
        }
        if (startedDisplayMillis < 0) {
            return false;
        }
        return (System.currentTimeMillis() - startedDisplayMillis) >= durationMillis;
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
    public int compareTo(@NotNull RelayActionBarNotificationImpl o) {
        // Higher priority first
        int pCompare = Integer.compare(o.priority.getLevel(), this.priority.getLevel());
        if (pCompare != 0) {
            return pCompare;
        }
        // Older creation time first (FIFO within same priority)
        return Long.compare(this.createdAtMillis, o.createdAtMillis);
    }
}
