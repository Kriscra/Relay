package org.vrz.relay.notification;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.notification.ActiveBossBar;
import org.vrz.relay.api.notification.BossBarBuilder;

import java.time.Duration;
import java.util.Objects;

public final class RelayBossBarBuilderImpl implements BossBarBuilder {

    private final RelayNotificationServiceImpl service;
    private final Player player;

    private Component title = Component.empty();
    private String templateMiniMessage;
    private BossBar.Color color = BossBar.Color.PURPLE;
    private BossBar.Overlay overlay = BossBar.Overlay.PROGRESS;
    private float progress = 1.0f;
    private boolean isCountdown = false;
    private boolean isCountup = false;
    private Duration duration = Duration.ofSeconds(5);
    private Runnable onComplete;

    public RelayBossBarBuilderImpl(@NotNull RelayNotificationServiceImpl service, @NotNull Player player) {
        this.service = Objects.requireNonNull(service, "service cannot be null");
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    @Override
    @NotNull
    public BossBarBuilder title(@NotNull Component title) {
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.templateMiniMessage = null;
        return this;
    }

    @Override
    @NotNull
    public BossBarBuilder titleMiniMessage(@NotNull String miniMessage) {
        Objects.requireNonNull(miniMessage, "miniMessage cannot be null");
        this.templateMiniMessage = miniMessage;
        this.title = MiniMessage.miniMessage().deserialize(miniMessage);
        return this;
    }

    @Override
    @NotNull
    public BossBarBuilder color(@NotNull BossBar.Color color) {
        this.color = Objects.requireNonNull(color, "color cannot be null");
        return this;
    }

    @Override
    @NotNull
    public BossBarBuilder overlay(@NotNull BossBar.Overlay overlay) {
        this.overlay = Objects.requireNonNull(overlay, "overlay cannot be null");
        return this;
    }

    @Override
    @NotNull
    public BossBarBuilder progress(float progress) {
        this.progress = Math.max(0.0f, Math.min(1.0f, progress));
        this.isCountdown = false;
        this.isCountup = false;
        return this;
    }

    @Override
    @NotNull
    public BossBarBuilder countdown(@NotNull Duration duration) {
        this.duration = Objects.requireNonNull(duration, "duration cannot be null");
        this.isCountdown = true;
        this.isCountup = false;
        this.progress = 1.0f;
        return this;
    }

    @Override
    @NotNull
    public BossBarBuilder countup(@NotNull Duration duration) {
        this.duration = Objects.requireNonNull(duration, "duration cannot be null");
        this.isCountdown = false;
        this.isCountup = true;
        this.progress = 0.0f;
        return this;
    }

    @Override
    @NotNull
    public BossBarBuilder onComplete(@NotNull Runnable onComplete) {
        this.onComplete = Objects.requireNonNull(onComplete, "onComplete cannot be null");
        return this;
    }

    @Override
    @NotNull
    public ActiveBossBar send(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        RelayActiveBossBarImpl activeBar = new RelayActiveBossBarImpl(
                service,
                plugin,
                player,
                title,
                templateMiniMessage,
                color,
                overlay,
                progress,
                isCountdown,
                isCountup,
                duration.toMillis(),
                onComplete
        );
        service.addBossBar(activeBar);
        return activeBar;
    }
}
