package org.vrz.relay.api.service;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Thread-safe service registry for dynamic dependency lookup, lifecycle coordination,
 * and service provision in Minecraft plugins.
 * <p>
 * Replaces Bukkit's legacy {@code ServicesManager} with modern generic contracts,
 * priority fallback chains, and reactive asynchronous readiness promises.
 */
public interface ServiceRegistry {

    /**
     * Registers a service provider with default {@link ServicePriority#NORMAL} priority.
     *
     * @param serviceClass the interface/class being implemented
     * @param provider     the provider instance
     * @param plugin       the plugin registering this service
     * @param <T>          the service type
     * @return the created service registration handle
     */
    @NotNull
    default <T> ServiceRegistration<T> register(@NotNull Class<T> serviceClass,
                                               @NotNull T provider,
                                               @NotNull Plugin plugin) {
        return register(serviceClass, provider, ServicePriority.NORMAL, plugin);
    }

    /**
     * Registers a service provider with a custom priority.
     *
     * @param serviceClass the interface/class being implemented
     * @param provider     the provider instance
     * @param priority     the service priority
     * @param plugin       the plugin registering this service
     * @param <T>          the service type
     * @return the created service registration handle
     */
    @NotNull
    <T> ServiceRegistration<T> register(@NotNull Class<T> serviceClass,
                                       @NotNull T provider,
                                       @NotNull ServicePriority priority,
                                       @NotNull Plugin plugin);

    /**
     * Unregisters a specific service registration.
     *
     * @param registration the registration handle to remove
     * @return true if successfully unregistered, false if not found
     */
    boolean unregister(@NotNull ServiceRegistration<?> registration);

    /**
     * Unregisters all services registered by the specified plugin.
     * <p>
     * Typically called during {@code Plugin#onDisable()}.
     *
     * @param plugin the plugin whose registrations should be purged
     */
    void unregisterAll(@NotNull Plugin plugin);

    /**
     * Retrieves the highest priority active provider for the given service class, if present.
     *
     * @param serviceClass the service interface to query
     * @param <T>          the service type
     * @return an Optional containing the active provider, or empty if none registered
     */
    @NotNull
    <T> Optional<T> getProvider(@NotNull Class<T> serviceClass);

    /**
     * Retrieves the highest priority active provider or throws {@link NoSuchElementException}.
     *
     * @param serviceClass the service interface to query
     * @param <T>          the service type
     * @return the active provider
     * @throws NoSuchElementException if no provider is registered
     */
    @NotNull
    default <T> T getRequiredProvider(@NotNull Class<T> serviceClass) {
        return getProvider(serviceClass)
                .orElseThrow(() -> new NoSuchElementException("No active provider registered for service: " + serviceClass.getName()));
    }

    /**
     * Gets the registration handle for the highest priority active provider.
     *
     * @param serviceClass the service interface to query
     * @param <T>          the service type
     * @return an Optional containing the registration handle
     */
    @NotNull
    <T> Optional<ServiceRegistration<T>> getRegistration(@NotNull Class<T> serviceClass);

    /**
     * Gets all registered providers for the given service class, ordered by priority descending.
     *
     * @param serviceClass the service interface to query
     * @param <T>          the service type
     * @return an unmodifiable collection of registrations ordered by priority
     */
    @NotNull
    <T> Collection<ServiceRegistration<T>> getRegistrations(@NotNull Class<T> serviceClass);

    /**
     * Returns whether any active provider is registered for the specified service class.
     *
     * @param serviceClass the service interface to check
     * @return true if a provider is available, false otherwise
     */
    boolean isProvided(@NotNull Class<?> serviceClass);

    /**
     * Gets a set of all service classes currently registered in this registry.
     *
     * @return unmodifiable set of known service interfaces
     */
    @NotNull
    Set<Class<?>> getKnownServices();

    /**
     * Asynchronously awaits the availability of a service provider.
     * <p>
     * If the service is already registered, the returned future is completed immediately.
     * Otherwise, it completes as soon as any plugin registers a provider for this service.
     *
     * <pre>{@code
     * registry.awaitService(EconomyService.class).thenAccept(economy -> {
     *     economy.deposit(player.getUniqueId(), BigDecimal.TEN, ...);
     * });
     * }</pre>
     *
     * @param serviceClass the service interface to await
     * @param <T>          the service type
     * @return a CompletableFuture completed with the service provider
     */
    @NotNull
    <T> CompletableFuture<T> awaitService(@NotNull Class<T> serviceClass);

    /**
     * Registers a callback to be invoked once the specified service becomes available.
     * If already available, the action is executed immediately.
     *
     * @param serviceClass the service interface to listen for
     * @param action       the callback consuming the provider
     * @param <T>          the service type
     */
    default <T> void onServiceReady(@NotNull Class<T> serviceClass, @NotNull Consumer<T> action) {
        awaitService(serviceClass).thenAccept(action);
    }
}
