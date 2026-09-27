package org.vrz.relay.menu;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.menu.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RelayMenuServiceImpl implements MenuService {

    private final Map<UUID, Menu> activeSessions = new ConcurrentHashMap<>();
    private final Set<Menu> registeredMenus = ConcurrentHashMap.newKeySet();

    @Override
    @NotNull
    public MenuBuilder createBuilder(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        return new RelayMenuBuilderImpl(plugin, this);
    }

    @Override
    @NotNull
    public <T> PaginatedMenuBuilder<T> createPaginatedBuilder(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        return new RelayPaginatedMenuBuilderImpl<>(plugin, this);
    }

    @Override
    @NotNull
    public MenuButtonBuilder createButtonBuilder() {
        return new RelayMenuButtonBuilderImpl();
    }

    @Override
    @NotNull
    public Optional<Menu> getOpenMenu(@NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        return Optional.ofNullable(activeSessions.get(player.getUniqueId()));
    }

    @Override
    public void closeOpenMenu(@NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        Menu menu = activeSessions.get(player.getUniqueId());
        if (menu != null) {
            menu.close(player);
        }
    }

    @Override
    @NotNull
    public Set<Menu> getActiveMenus() {
        return Collections.unmodifiableSet(registeredMenus);
    }

    @Override
    public void closeAll() {
        for (Menu menu : registeredMenus) {
            menu.closeAll();
        }
        activeSessions.clear();
        registeredMenus.clear();
    }

    @Override
    public void closeAll(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        for (Menu menu : new ArrayList<>(registeredMenus)) {
            if (menu.getPlugin().equals(plugin)) {
                menu.closeAll();
                registeredMenus.remove(menu);
            }
        }
    }

    void registerSession(@NotNull UUID playerId, @NotNull Menu menu) {
        activeSessions.put(playerId, menu);
    }

    void unregisterSession(@NotNull UUID playerId) {
        activeSessions.remove(playerId);
    }

    void registerMenu(@NotNull Menu menu) {
        registeredMenus.add(menu);
    }

    void unregisterMenu(@NotNull Menu menu) {
        registeredMenus.remove(menu);
    }
}
