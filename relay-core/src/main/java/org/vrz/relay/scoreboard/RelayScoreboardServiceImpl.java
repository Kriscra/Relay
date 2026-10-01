package org.vrz.relay.scoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.scheduler.SchedulerService;
import org.vrz.relay.api.scoreboard.ScoreboardService;
import org.vrz.relay.api.scoreboard.Sidebar;
import org.vrz.relay.api.scoreboard.SidebarBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * High-performance, thread-safe implementation of {@link ScoreboardService}.
 * <p>
 * Manages per-player sidebar priority stacks, dynamic update loops,
 * Tablist header/footer styling, and automatic zero-leak cleanup.
 */
public class RelayScoreboardServiceImpl implements ScoreboardService, Listener {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final SchedulerService scheduler;
    private final Map<UUID, List<RelaySidebarImpl>> playerSidebarStacks = new ConcurrentHashMap<>();
    private final Map<Plugin, Set<RelaySidebarImpl>> pluginSidebars = new ConcurrentHashMap<>();
    private final Map<UUID, RelaySidebarImpl> activeSidebarsById = new ConcurrentHashMap<>();

    public RelayScoreboardServiceImpl(@Nullable SchedulerService scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    @NotNull
    public SidebarBuilder sidebarBuilder(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        return new RelaySidebarBuilderImpl(plugin, scheduler, this::registerSidebar, this::unregisterSidebar);
    }

    private void registerSidebar(@NotNull RelaySidebarImpl sidebar) {
        activeSidebarsById.put(sidebar.getId(), sidebar);

        pluginSidebars.computeIfAbsent(sidebar.getPlugin(), k -> ConcurrentHashMap.newKeySet())
                .add(sidebar);

        Player player = sidebar.getPlayer();
        if (player != null) {
            List<RelaySidebarImpl> stack = playerSidebarStacks.computeIfAbsent(
                    player.getUniqueId(),
                    k -> new CopyOnWriteArrayList<>()
            );

            // Add and sort by priority descending
            stack.add(sidebar);
            stack.sort(Comparator.comparingInt(RelaySidebarImpl::getPriority).reversed());

            // If this sidebar is now the highest priority, show it to the player
            if (!stack.isEmpty() && stack.get(0).equals(sidebar)) {
                sidebar.show(player);
            }
        }
    }

    private void unregisterSidebar(@NotNull RelaySidebarImpl sidebar) {
        activeSidebarsById.remove(sidebar.getId());

        Set<RelaySidebarImpl> byPlugin = pluginSidebars.get(sidebar.getPlugin());
        if (byPlugin != null) {
            byPlugin.remove(sidebar);
            if (byPlugin.isEmpty()) {
                pluginSidebars.remove(sidebar.getPlugin());
            }
        }

        Player player = sidebar.getPlayer();
        if (player != null) {
            List<RelaySidebarImpl> stack = playerSidebarStacks.get(player.getUniqueId());
            if (stack != null) {
                stack.remove(sidebar);
                if (stack.isEmpty()) {
                    playerSidebarStacks.remove(player.getUniqueId());
                } else if (player.isOnline()) {
                    // Activate the next highest priority sidebar in the stack
                    RelaySidebarImpl next = stack.get(0);
                    next.show(player);
                }
            }
        }
    }

    @Override
    @NotNull
    public Optional<Sidebar> getSidebar(@NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        List<RelaySidebarImpl> stack = playerSidebarStacks.get(player.getUniqueId());
        if (stack != null && !stack.isEmpty()) {
            return Optional.of(stack.get(0));
        }
        return Optional.empty();
    }

    @Override
    public void removeSidebar(@NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        List<RelaySidebarImpl> stack = playerSidebarStacks.get(player.getUniqueId());
        if (stack != null && !stack.isEmpty()) {
            RelaySidebarImpl active = stack.get(0);
            active.destroy();
            unregisterSidebar(active);
        }
    }

    @Override
    public void setTablist(@NotNull Player player, @Nullable Component header, @Nullable Component footer) {
        Objects.requireNonNull(player, "player cannot be null");
        Component h = header != null ? header : Component.empty();
        Component f = footer != null ? footer : Component.empty();

        if (scheduler != null && Bukkit.getServer() != null) {
            scheduler.runFor(Bukkit.getPluginManager().getPlugins()[0], player, () -> {
                if (player.isOnline()) {
                    player.sendPlayerListHeaderAndFooter(h, f);
                }
            });
        } else if (player.isOnline()) {
            player.sendPlayerListHeaderAndFooter(h, f);
        }
    }

    @Override
    public void setTablistMiniMessage(@NotNull Player player, @Nullable String header, @Nullable String footer) {
        Component h = header != null && !header.isBlank() ? MINI_MESSAGE.deserialize(header) : null;
        Component f = footer != null && !footer.isBlank() ? MINI_MESSAGE.deserialize(footer) : null;
        setTablist(player, h, f);
    }

    @Override
    public void resetTablist(@NotNull Player player) {
        setTablist(player, Component.empty(), Component.empty());
    }

    @Override
    public void clearAll(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Set<RelaySidebarImpl> sidebars = pluginSidebars.get(plugin);
        if (sidebars != null) {
            for (RelaySidebarImpl sidebar : new ArrayList<>(sidebars)) {
                sidebar.destroy();
            }
        }
    }

    @Override
    public void clearAll() {
        for (RelaySidebarImpl sidebar : new ArrayList<>(activeSidebarsById.values())) {
            sidebar.destroy();
        }
        activeSidebarsById.clear();
        playerSidebarStacks.clear();
        pluginSidebars.clear();
    }

    @Override
    public int getActiveSidebarCount() {
        return activeSidebarsById.size();
    }

    @Override
    @NotNull
    public Collection<Sidebar> getActiveSidebars() {
        return Collections.unmodifiableCollection(new ArrayList<>(activeSidebarsById.values()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        List<RelaySidebarImpl> stack = playerSidebarStacks.get(uuid);
        if (stack != null) {
            for (RelaySidebarImpl sidebar : new ArrayList<>(stack)) {
                sidebar.destroy();
            }
        }
    }
}
