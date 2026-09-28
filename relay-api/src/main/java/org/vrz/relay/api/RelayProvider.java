package org.vrz.relay.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.cooldown.CooldownService;
import org.vrz.relay.api.data.SharedDataService;
import org.vrz.relay.api.economy.EconomyService;
import org.vrz.relay.api.messenger.RelayMessenger;
import org.vrz.relay.api.service.ServiceRegistry;

import java.util.Optional;

/**
 * Underlying runtime bridge providing access to Relay systems.
 * Implemented by Relay-Core.
 */
public interface RelayProvider {

    /**
     * Gets the active service registry.
     *
     * @return service registry instance
     */
    @NotNull
    ServiceRegistry getServiceRegistry();

    /**
     * Gets the active inter-plugin messenger event bus.
     *
     * @return messenger instance
     */
    @NotNull
    RelayMessenger getMessenger();

    /**
     * Gets the global cooldown service.
     *
     * @return cooldown service instance
     */
    @NotNull
    CooldownService getCooldownService();

    /**
     * Gets the global shared data and metadata store.
     *
     * @return shared data service instance
     */
    @NotNull
    SharedDataService getDataService();

    /**
     * Gets the global metrics and telemetry service.
     *
     * @return metrics service instance
     */
    @NotNull
    org.vrz.relay.api.metric.MetricsService getMetricsService();

    /**
     * Gets the global hologram service.
     *
     * @return hologram service instance
     */
    @NotNull
    org.vrz.relay.api.hologram.HologramService getHologramService();

    /**
     * Gets the global menu and virtual GUI service.
     *
     * @return menu service instance
     */
    @NotNull
    org.vrz.relay.api.menu.MenuService getMenuService();

    /**
     * Gets the global notification and player feedback service.
     *
     * @return notification service instance
     */
    @NotNull
    org.vrz.relay.api.notification.NotificationService getNotificationService();

    /**
     * Gets the primary registered economy service, if one is registered.
     *
     * @return optional containing active EconomyService
     */
    @NotNull
    default Optional<EconomyService> getEconomy() {
        return getServiceRegistry().getProvider(EconomyService.class);
    }

    /**
     * Gets the primary registered permission service, if one is registered.
     *
     * @return optional containing active PermissionService
     */
    @NotNull
    default Optional<org.vrz.relay.api.permission.PermissionService> getPermission() {
        return getServiceRegistry().getProvider(org.vrz.relay.api.permission.PermissionService.class);
    }

    /**
     * Gets the primary registered party service, if one is registered.
     *
     * @return optional containing active PartyService
     */
    @NotNull
    default Optional<org.vrz.relay.api.party.PartyService> getPartyService() {
        return getServiceRegistry().getProvider(org.vrz.relay.api.party.PartyService.class);
    }
}

