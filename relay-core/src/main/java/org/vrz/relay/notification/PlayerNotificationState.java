package org.vrz.relay.notification;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.notification.ContinuousActionBarProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public final class PlayerNotificationState {

    private final Player player;

    private final PriorityBlockingQueue<RelayActionBarNotificationImpl> actionBarQueue = new PriorityBlockingQueue<>();
    private final AtomicReference<RelayActionBarNotificationImpl> activeActionBar = new AtomicReference<>(null);

    private final PriorityBlockingQueue<RelayTitleNotificationImpl> titleQueue = new PriorityBlockingQueue<>();
    private final AtomicReference<RelayTitleNotificationImpl> activeTitle = new AtomicReference<>(null);

    private final Set<RelayActiveBossBarImpl> activeBossBars = ConcurrentHashMap.newKeySet();
    private final Map<Plugin, ContinuousActionBarProvider> continuousProviders = new ConcurrentHashMap<>();

    public PlayerNotificationState(@NotNull Player player) {
        this.player = player;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    public void enqueueActionBar(@NotNull RelayActionBarNotificationImpl notification) {
        RelayActionBarNotificationImpl current = activeActionBar.get();
        if (current == null) {
            notification.markStarted();
            activeActionBar.set(notification);
            try {
                player.sendActionBar(notification.getMessage());
                if (notification.getSound() != null) {
                    player.playSound(notification.getSound());
                }
            } catch (Throwable ignored) {}
        } else if (notification.getPriority().isHigherThan(current.getPriority())) {
            // Preemption: Interrupt lower priority
            actionBarQueue.add(current);
            notification.markStarted();
            activeActionBar.set(notification);
            try {
                player.sendActionBar(notification.getMessage());
                if (notification.getSound() != null) {
                    player.playSound(notification.getSound());
                }
            } catch (Throwable ignored) {}
        } else {
            actionBarQueue.add(notification);
        }
    }

    public void enqueueTitle(@NotNull RelayTitleNotificationImpl notification) {
        RelayTitleNotificationImpl current = activeTitle.get();
        if (current == null) {
            notification.markStarted();
            activeTitle.set(notification);
            try {
                Title.Times times = Title.Times.times(notification.getFadeIn(), notification.getStay(), notification.getFadeOut());
                player.showTitle(Title.title(notification.getTitle(), notification.getSubtitle(), times));
                if (notification.getSound() != null) {
                    player.playSound(notification.getSound());
                }
            } catch (Throwable ignored) {}
        } else if (notification.getPriority().isHigherThan(current.getPriority())) {
            titleQueue.add(current);
            notification.markStarted();
            activeTitle.set(notification);
            try {
                Title.Times times = Title.Times.times(notification.getFadeIn(), notification.getStay(), notification.getFadeOut());
                player.showTitle(Title.title(notification.getTitle(), notification.getSubtitle(), times));
                if (notification.getSound() != null) {
                    player.playSound(notification.getSound());
                }
            } catch (Throwable ignored) {}
        } else {
            titleQueue.add(notification);
        }
    }

    public void addBossBar(@NotNull RelayActiveBossBarImpl bossBar) {
        activeBossBars.add(bossBar);
        try {
            player.showBossBar(bossBar.getAdventureBar());
        } catch (Throwable ignored) {}
    }

    public void removeBossBar(@NotNull RelayActiveBossBarImpl bossBar) {
        activeBossBars.remove(bossBar);
    }

    @NotNull
    public Collection<RelayActiveBossBarImpl> getBossBars() {
        return Collections.unmodifiableCollection(activeBossBars);
    }

    public void registerContinuous(@NotNull Plugin plugin, @NotNull ContinuousActionBarProvider provider) {
        continuousProviders.put(plugin, provider);
    }

    public void unregisterContinuous(@NotNull Plugin plugin) {
        continuousProviders.remove(plugin);
    }

    @Nullable
    public RelayActionBarNotificationImpl getActiveActionBar() {
        return activeActionBar.get();
    }

    public void tick(long now) {
        if (!player.isOnline()) {
            return;
        }

        tickBossBars(now);
        tickActionBar();
        tickTitles();
    }

    private void tickBossBars(long now) {
        for (RelayActiveBossBarImpl bar : activeBossBars) {
            bar.tick(now);
            if (!bar.isVisible()) {
                activeBossBars.remove(bar);
            }
        }
    }

    private void tickActionBar() {
        RelayActionBarNotificationImpl active = activeActionBar.get();
        if (active != null) {
            if (active.isExpired()) {
                active.triggerComplete();
                activeActionBar.compareAndSet(active, null);
                active = null;
            } else {
                // Check if higher priority waiting
                RelayActionBarNotificationImpl head = actionBarQueue.peek();
                if (head != null && head.getPriority().isHigherThan(active.getPriority())) {
                    actionBarQueue.poll();
                    actionBarQueue.add(active);
                    head.markStarted();
                    activeActionBar.set(head);
                    active = head;
                    try {
                        player.sendActionBar(active.getMessage());
                        if (active.getSound() != null) {
                            player.playSound(active.getSound());
                        }
                    } catch (Throwable ignored) {}
                } else {
                    // Refresh action bar so client keeps rendering it smoothly
                    try {
                        player.sendActionBar(active.getMessage());
                    } catch (Throwable ignored) {}
                }
            }
        }

        if (active == null) {
            RelayActionBarNotificationImpl next = actionBarQueue.poll();
            while (next != null && next.isExpired()) {
                next = actionBarQueue.poll();
            }

            if (next != null) {
                next.markStarted();
                activeActionBar.set(next);
                try {
                    player.sendActionBar(next.getMessage());
                    if (next.getSound() != null) {
                        player.playSound(next.getSound());
                    }
                } catch (Throwable ignored) {}
            } else if (!continuousProviders.isEmpty()) {
                // Continuous background feed fallback
                for (ContinuousActionBarProvider provider : continuousProviders.values()) {
                    try {
                        Component comp = provider.provide(player);
                        if (comp != null) {
                            player.sendActionBar(comp);
                            break;
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    private void tickTitles() {
        RelayTitleNotificationImpl active = activeTitle.get();
        if (active != null) {
            if (active.isExpired()) {
                active.triggerComplete();
                activeTitle.compareAndSet(active, null);
                active = null;
            }
        }

        if (active == null) {
            RelayTitleNotificationImpl next = titleQueue.poll();
            while (next != null && next.isExpired()) {
                next = titleQueue.poll();
            }

            if (next != null) {
                next.markStarted();
                activeTitle.set(next);
                try {
                    Title.Times times = Title.Times.times(next.getFadeIn(), next.getStay(), next.getFadeOut());
                    player.showTitle(Title.title(next.getTitle(), next.getSubtitle(), times));
                    if (next.getSound() != null) {
                        player.playSound(next.getSound());
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    public void clearAll() {
        RelayActionBarNotificationImpl act = activeActionBar.getAndSet(null);
        if (act != null) {
            act.cancel();
        }
        actionBarQueue.clear();

        RelayTitleNotificationImpl title = activeTitle.getAndSet(null);
        if (title != null) {
            title.cancel();
        }
        titleQueue.clear();

        for (RelayActiveBossBarImpl bar : activeBossBars) {
            bar.dismiss();
        }
        activeBossBars.clear();
        continuousProviders.clear();
    }

    public void clearPlugin(@NotNull Plugin plugin) {
        RelayActionBarNotificationImpl act = activeActionBar.get();
        if (act != null && act.getPlugin().equals(plugin)) {
            act.cancel();
            activeActionBar.compareAndSet(act, null);
        }
        actionBarQueue.removeIf(n -> n.getPlugin().equals(plugin));

        RelayTitleNotificationImpl title = activeTitle.get();
        if (title != null && title.getPlugin().equals(plugin)) {
            title.cancel();
            activeTitle.compareAndSet(title, null);
        }
        titleQueue.removeIf(t -> t.getPlugin().equals(plugin));

        for (RelayActiveBossBarImpl bar : activeBossBars) {
            if (bar.getPlugin().equals(plugin)) {
                bar.dismiss();
            }
        }
        continuousProviders.remove(plugin);
    }
}
