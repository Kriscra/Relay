package org.vrz.relay.notification;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.notification.ActiveBossBar;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RelayActiveBossBarImpl implements ActiveBossBar {

    private final RelayNotificationServiceImpl service;
    private final Plugin plugin;
    private final Player player;
    private final BossBar adventureBar;
    private final String templateMiniMessage;
    private final boolean isCountdown;
    private final boolean isCountup;
    private final long durationMillis;
    private final Runnable onComplete;
    private final long startTimeMillis;
    private final AtomicBoolean dismissed = new AtomicBoolean(false);

    private volatile float progress;

    public RelayActiveBossBarImpl(@NotNull RelayNotificationServiceImpl service,
                                  @NotNull Plugin plugin,
                                  @NotNull Player player,
                                  @NotNull Component initialTitle,
                                  @Nullable String templateMiniMessage,
                                  @NotNull BossBar.Color color,
                                  @NotNull BossBar.Overlay overlay,
                                  float initialProgress,
                                  boolean isCountdown,
                                  boolean isCountup,
                                  long durationMillis,
                                  @Nullable Runnable onComplete) {
        this.service = Objects.requireNonNull(service, "service cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.templateMiniMessage = templateMiniMessage;
        this.isCountdown = isCountdown;
        this.isCountup = isCountup;
        this.durationMillis = durationMillis;
        this.onComplete = onComplete;
        this.startTimeMillis = System.currentTimeMillis();
        this.progress = Math.max(0.0f, Math.min(1.0f, initialProgress));

        this.adventureBar = BossBar.bossBar(initialTitle, this.progress, color, overlay);
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
    public BossBar getAdventureBar() {
        return adventureBar;
    }

    @Override
    public void setTitle(@NotNull Component title) {
        adventureBar.name(Objects.requireNonNull(title, "title cannot be null"));
    }

    @Override
    public void setTitleMiniMessage(@NotNull String miniMessage) {
        adventureBar.name(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(miniMessage, "miniMessage cannot be null")));
    }

    @Override
    public void setProgress(float progress) {
        this.progress = Math.max(0.0f, Math.min(1.0f, progress));
        adventureBar.progress(this.progress);
    }

    @Override
    public float getProgress() {
        return progress;
    }

    @Override
    public void setColor(@NotNull BossBar.Color color) {
        adventureBar.color(Objects.requireNonNull(color, "color cannot be null"));
    }

    @Override
    public long getRemainingMillis() {
        if (dismissed.get()) {
            return 0;
        }
        if (!isCountdown && !isCountup) {
            return -1;
        }
        long elapsed = System.currentTimeMillis() - startTimeMillis;
        return Math.max(0, durationMillis - elapsed);
    }

    @Override
    public boolean isVisible() {
        return !dismissed.get();
    }

    @Override
    public void dismiss() {
        if (dismissed.compareAndSet(false, true)) {
            try {
                player.hideBossBar(adventureBar);
            } catch (Throwable ignored) {}
            service.removeBossBar(this);
        }
    }

    public void tick(long now) {
        if (dismissed.get()) {
            return;
        }

        if (isCountdown || isCountup) {
            long elapsed = now - startTimeMillis;
            if (elapsed >= durationMillis) {
                // Completed
                setProgress(isCountdown ? 0.0f : 1.0f);
                if (onComplete != null) {
                    try {
                        onComplete.run();
                    } catch (Throwable ignored) {}
                }
                dismiss();
                return;
            }

            float currentProgress;
            if (isCountdown) {
                currentProgress = Math.max(0.0f, 1.0f - ((float) elapsed / (float) durationMillis));
            } else {
                currentProgress = Math.min(1.0f, (float) elapsed / (float) durationMillis);
            }
            setProgress(currentProgress);

            if (templateMiniMessage != null && !templateMiniMessage.isEmpty()) {
                long remainingMillis = Math.max(0, durationMillis - elapsed);
                long secondsLeft = (long) Math.ceil(remainingMillis / 1000.0);
                int percent = Math.round(currentProgress * 100);

                String formatted = templateMiniMessage
                        .replace("%time%", String.valueOf(secondsLeft))
                        .replace("%progress%", percent + "%");

                adventureBar.name(MiniMessage.miniMessage().deserialize(formatted));
            }
        }
    }
}
