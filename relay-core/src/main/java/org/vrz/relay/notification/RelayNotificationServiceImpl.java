package org.vrz.relay.notification;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.notification.ActionBarBuilder;
import org.vrz.relay.api.notification.ActionBarNotification;
import org.vrz.relay.api.notification.ActiveBossBar;
import org.vrz.relay.api.notification.BossBarBuilder;
import org.vrz.relay.api.notification.ContinuousActionBarProvider;
import org.vrz.relay.api.notification.NotificationService;
import org.vrz.relay.api.notification.TitleBuilder;
import org.vrz.relay.api.notification.ToastBuilder;
import org.vrz.relay.util.RelaySchedulerBridge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RelayNotificationServiceImpl implements NotificationService {

    private final Plugin plugin;
    private final Map<UUID, PlayerNotificationState> states = new ConcurrentHashMap<>();
    private final Runnable cancelTicker;

    public RelayNotificationServiceImpl(@NotNull Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        // Tick every 2 ticks (~100ms) for smooth progress and zero CPU overhead
        this.cancelTicker = RelaySchedulerBridge.runTimer(plugin, this::tick, 2L, 2L);
    }

    // For unit tests without running Bukkit server
    public RelayNotificationServiceImpl() {
        this.plugin = null;
        this.cancelTicker = () -> {};
    }

    @NotNull
    private PlayerNotificationState getOrCreateState(@NotNull Player player) {
        return states.computeIfAbsent(player.getUniqueId(), k -> new PlayerNotificationState(player));
    }

    @Override
    @NotNull
    public ActionBarBuilder actionBar(@NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        return new RelayActionBarBuilderImpl(this, player);
    }

    @Override
    @NotNull
    public TitleBuilder title(@NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        return new RelayTitleBuilderImpl(this, player);
    }

    @Override
    @NotNull
    public BossBarBuilder bossBar(@NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        return new RelayBossBarBuilderImpl(this, player);
    }

    @Override
    @NotNull
    public ToastBuilder toast(@NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        return new RelayToastBuilderImpl(this, player);
    }

    @Override
    public void registerContinuousActionBar(@NotNull Plugin plugin, @NotNull Player player, @NotNull ContinuousActionBarProvider provider) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(provider, "provider cannot be null");
        getOrCreateState(player).registerContinuous(plugin, provider);
    }

    @Override
    public void unregisterContinuousActionBar(@NotNull Plugin plugin, @NotNull Player player) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(player, "player cannot be null");
        PlayerNotificationState state = states.get(player.getUniqueId());
        if (state != null) {
            state.unregisterContinuous(plugin);
        }
    }

    @Override
    @NotNull
    public Optional<ActionBarNotification> getActiveActionBar(@NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        PlayerNotificationState state = states.get(player.getUniqueId());
        if (state == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(state.getActiveActionBar());
    }

    @Override
    @NotNull
    public Collection<ActiveBossBar> getActiveBossBars(@NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        PlayerNotificationState state = states.get(player.getUniqueId());
        if (state == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(state.getBossBars());
    }

    public void enqueueActionBar(@NotNull RelayActionBarNotificationImpl notification) {
        getOrCreateState(notification.getPlayer()).enqueueActionBar(notification);
    }

    public void enqueueTitle(@NotNull RelayTitleNotificationImpl notification) {
        getOrCreateState(notification.getPlayer()).enqueueTitle(notification);
    }

    public void addBossBar(@NotNull RelayActiveBossBarImpl bossBar) {
        getOrCreateState(bossBar.getPlayer()).addBossBar(bossBar);
    }

    public void removeBossBar(@NotNull RelayActiveBossBarImpl bossBar) {
        PlayerNotificationState state = states.get(bossBar.getPlayer().getUniqueId());
        if (state != null) {
            state.removeBossBar(bossBar);
        }
    }

    public void handlePlayerQuit(@NotNull Player player) {
        PlayerNotificationState state = states.remove(player.getUniqueId());
        if (state != null) {
            state.clearAll();
        }
    }

    @Override
    public void clearAll(@NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        PlayerNotificationState state = states.get(player.getUniqueId());
        if (state != null) {
            state.clearAll();
        }
    }

    @Override
    public void clearAll(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        for (PlayerNotificationState state : states.values()) {
            state.clearPlugin(plugin);
        }
    }

    public void tick() {
        long now = System.currentTimeMillis();
        for (PlayerNotificationState state : states.values()) {
            state.tick(now);
        }
    }

    @Override
    public void shutdown() {
        if (cancelTicker != null) {
            try {
                cancelTicker.run();
            } catch (Throwable ignored) {}
        }
        for (PlayerNotificationState state : states.values()) {
            state.clearAll();
        }
        states.clear();
    }
}
