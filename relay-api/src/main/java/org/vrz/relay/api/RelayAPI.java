package org.vrz.relay.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.economy.EconomyService;
import org.vrz.relay.api.messenger.RelayMessenger;
import org.vrz.relay.api.service.ServiceRegistry;

import java.util.Objects;
import java.util.Optional;

/**
 * Static entry point and public facade for the Relay framework.
 * <p>
 * Provides global, thread-safe access to:
 * <ul>
 *   <li>{@link ServiceRegistry}: Reactive provider registry with priority fallback chains</li>
 *   <li>{@link RelayMessenger}: Topic-based inter-plugin publish-subscribe messaging backbone</li>
 *   <li>{@link EconomyService}: Modern async multi-currency economy standard</li>
 * </ul>
 *
 * <pre>{@code
 * // Accessing the Service Registry
 * ServiceRegistry registry = RelayAPI.getServiceRegistry();
 *
 * // Publishing a message across plugins
 * RelayAPI.getMessenger().publish("game:started", new GameStatePayload(...));
 *
 * // Accessing Economy
 * RelayAPI.getEconomy().ifPresent(econ -> {
 *     econ.getBalance(player.getUniqueId()).thenAccept(balance -> ...);
 * });
 * }</pre>
 */
public final class RelayAPI {

    private static volatile RelayProvider instance;

    private RelayAPI() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Checks if the Relay core runtime has initialized the API.
     *
     * @return true if initialized and ready for consumption
     */
    public static boolean isInitialized() {
        return instance != null;
    }

    /**
     * Internal framework method used by Relay-Core to bind the runtime instance.
     *
     * @param provider the core provider implementation
     */
    @ApiStatus.Internal
    public static void setInstance(@NotNull RelayProvider provider) {
        instance = Objects.requireNonNull(provider, "RelayProvider cannot be null");
    }

    /**
     * Internal framework method to unbind runtime on server shutdown.
     */
    @ApiStatus.Internal
    public static void clearInstance() {
        instance = null;
    }

    @NotNull
    private static RelayProvider getInstance() {
        RelayProvider current = instance;
        if (current == null) {
            throw new IllegalStateException("RelayAPI has not been initialized yet! Ensure Relay is loaded on your server.");
        }
        return current;
    }

    /**
     * Gets the global {@link ServiceRegistry}.
     *
     * @return active service registry
     * @throws IllegalStateException if Relay is not yet initialized
     */
    @NotNull
    public static ServiceRegistry getServiceRegistry() {
        return getInstance().getServiceRegistry();
    }

    /**
     * Gets the global {@link RelayMessenger}.
     *
     * @return active inter-plugin messenger
     * @throws IllegalStateException if Relay is not yet initialized
     */
    @NotNull
    public static RelayMessenger getMessenger() {
        return getInstance().getMessenger();
    }

    /**
     * Convenience method to retrieve the active {@link EconomyService} provider, if registered.
     *
     * @return optional containing the active EconomyService
     * @throws IllegalStateException if Relay is not yet initialized
     */
    @NotNull
    public static Optional<EconomyService> getEconomy() {
        return getInstance().getEconomy();
    }

    /**
     * Gets the global {@link org.vrz.relay.api.cooldown.CooldownService}.
     *
     * @return active cooldown service
     * @throws IllegalStateException if Relay is not yet initialized
     */
    @NotNull
    public static org.vrz.relay.api.cooldown.CooldownService getCooldowns() {
        return getInstance().getCooldownService();
    }

    /**
     * Gets the global {@link org.vrz.relay.api.data.SharedDataService}.
     *
     * @return active shared data service
     * @throws IllegalStateException if Relay is not yet initialized
     */
    @NotNull
    public static org.vrz.relay.api.data.SharedDataService getData() {
        return getInstance().getDataService();
    }

    /**
     * Gets the global {@link org.vrz.relay.api.metric.MetricsService}.
     *
     * @return active metrics and telemetry service
     * @throws IllegalStateException if Relay is not yet initialized
     */
    @NotNull
    public static org.vrz.relay.api.metric.MetricsService getMetrics() {
        return getInstance().getMetricsService();
    }

    /**
     * Convenience method to retrieve the active {@link org.vrz.relay.api.permission.PermissionService} provider, if registered.
     *
     * @return optional containing the active PermissionService
     * @throws IllegalStateException if Relay is not yet initialized
     */
    @NotNull
    public static Optional<org.vrz.relay.api.permission.PermissionService> getPermissions() {
        return getInstance().getPermission();
    }

    /**
     * Convenience method to retrieve the active {@link org.vrz.relay.api.party.PartyService} provider, if registered.
     *
     * @return optional containing the active PartyService
     * @throws IllegalStateException if Relay is not yet initialized
     */
    @NotNull
    public static Optional<org.vrz.relay.api.party.PartyService> getParties() {
        return getInstance().getPartyService();
    }

    /**
     * Gets the global {@link org.vrz.relay.api.hologram.HologramService}.
     *
     * @return active hologram service
     * @throws IllegalStateException if Relay is not yet initialized
     */
    @NotNull
    public static org.vrz.relay.api.hologram.HologramService getHolograms() {
        return getInstance().getHologramService();
    }

    /**
     * Gets the global {@link org.vrz.relay.api.menu.MenuService}.
     *
     * @return active menu service
     * @throws IllegalStateException if Relay is not yet initialized
     */
    @NotNull
    public static org.vrz.relay.api.menu.MenuService getMenus() {
        return getInstance().getMenuService();
    }

    /**
     * Gets the global {@link org.vrz.relay.api.notification.NotificationService}.
     *
     * @return active notification service
     * @throws IllegalStateException if Relay is not yet initialized
     */
    @NotNull
    public static org.vrz.relay.api.notification.NotificationService getNotifications() {
        return getInstance().getNotificationService();
    }

    /**
     * Gets the global unified {@link org.vrz.relay.api.scheduler.SchedulerService}.
     *
     * @return active scheduler service
     * @throws IllegalStateException if Relay is not yet initialized
     */
    @NotNull
    public static org.vrz.relay.api.scheduler.SchedulerService getScheduler() {
        return getInstance().getSchedulerService();
    }
}

