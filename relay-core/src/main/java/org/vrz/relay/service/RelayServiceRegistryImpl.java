package org.vrz.relay.service;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.event.PrimaryProviderChangedEvent;
import org.vrz.relay.api.event.ServiceRegisteredEvent;
import org.vrz.relay.api.event.ServiceUnregisteredEvent;
import org.vrz.relay.api.service.ServicePriority;
import org.vrz.relay.api.service.ServiceRegistration;
import org.vrz.relay.api.service.ServiceRegistry;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * High-performance, concurrent implementation of {@link ServiceRegistry}.
 */
public class RelayServiceRegistryImpl implements ServiceRegistry {

    private static final Logger LOGGER = Logger.getLogger("RelayServiceRegistry");

    private final Map<Class<?>, List<ServiceRegistration<?>>> providers = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<CompletableFuture<Object>>> pendingAwaits = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> ServiceRegistration<T> register(@NotNull Class<T> serviceClass,
                                              @NotNull T provider,
                                              @NotNull ServicePriority priority,
                                              @NotNull Plugin plugin) {
        Objects.requireNonNull(serviceClass, "serviceClass cannot be null");
        Objects.requireNonNull(provider, "provider cannot be null");
        Objects.requireNonNull(priority, "priority cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");

        RelayServiceRegistrationImpl<T> reg = new RelayServiceRegistrationImpl<>(serviceClass, provider, priority, plugin);
        boolean becamePrimary;
        ServiceRegistration<T> oldPrimary = null;

        synchronized (lock) {
            List<ServiceRegistration<?>> list = providers.computeIfAbsent(serviceClass, k -> new CopyOnWriteArrayList<>());
            if (!list.isEmpty()) {
                oldPrimary = (ServiceRegistration<T>) list.getFirst();
            }

            list.add(reg);
            // Sort by priority descending, then registration time
            list.sort((Comparator) Comparator.naturalOrder());

            ServiceRegistration<T> newPrimary = (ServiceRegistration<T>) list.getFirst();
            becamePrimary = newPrimary.equals(reg);

            // Complete any pending futures
            List<CompletableFuture<Object>> futures = pendingAwaits.remove(serviceClass);
            if (futures != null) {
                for (CompletableFuture<Object> future : futures) {
                    future.complete(newPrimary.getProvider());
                }
            }

            fireEvent(new ServiceRegisteredEvent<>(reg, becamePrimary));

            if (becamePrimary && (oldPrimary == null || !oldPrimary.equals(reg))) {
                fireEvent(new PrimaryProviderChangedEvent<>(serviceClass, oldPrimary, newPrimary));
            }
        }

        return reg;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean unregister(@NotNull ServiceRegistration<?> registration) {
        Objects.requireNonNull(registration, "registration cannot be null");
        Class<?> serviceClass = registration.getServiceClass();

        synchronized (lock) {
            List<ServiceRegistration<?>> list = providers.get(serviceClass);
            if (list == null || !list.remove(registration)) {
                return false;
            }

            ServiceRegistration<?> oldPrimary = registration;
            ServiceRegistration<?> newPrimary = list.isEmpty() ? null : list.getFirst();

            if (list.isEmpty()) {
                providers.remove(serviceClass);
            }

            fireEvent(new ServiceUnregisteredEvent<>((ServiceRegistration<Object>) registration,
                    (ServiceRegistration<Object>) newPrimary));

            if (newPrimary != null && !newPrimary.equals(oldPrimary)) {
                fireEvent(new PrimaryProviderChangedEvent<>((Class<Object>) serviceClass,
                        (ServiceRegistration<Object>) oldPrimary,
                        (ServiceRegistration<Object>) newPrimary));
            }

            return true;
        }
    }

    @Override
    public void unregisterAll(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");

        synchronized (lock) {
            for (Map.Entry<Class<?>, List<ServiceRegistration<?>>> entry : providers.entrySet()) {
                List<ServiceRegistration<?>> list = entry.getValue();
                List<ServiceRegistration<?>> toRemove = new ArrayList<>();
                for (ServiceRegistration<?> reg : list) {
                    if (reg.getPlugin().equals(plugin)) {
                        toRemove.add(reg);
                    }
                }
                for (ServiceRegistration<?> reg : toRemove) {
                    unregister(reg);
                }
            }
        }
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProvider(@NotNull Class<T> serviceClass) {
        Objects.requireNonNull(serviceClass, "serviceClass cannot be null");
        List<ServiceRegistration<?>> list = providers.get(serviceClass);
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of((T) list.getFirst().getProvider());
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> Optional<ServiceRegistration<T>> getRegistration(@NotNull Class<T> serviceClass) {
        Objects.requireNonNull(serviceClass, "serviceClass cannot be null");
        List<ServiceRegistration<?>> list = providers.get(serviceClass);
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of((ServiceRegistration<T>) list.getFirst());
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> Collection<ServiceRegistration<T>> getRegistrations(@NotNull Class<T> serviceClass) {
        Objects.requireNonNull(serviceClass, "serviceClass cannot be null");
        List<ServiceRegistration<?>> list = providers.get(serviceClass);
        if (list == null) {
            return Collections.emptyList();
        }
        return (Collection<ServiceRegistration<T>>) (Collection<?>) Collections.unmodifiableList(new ArrayList<>(list));
    }

    @Override
    public boolean isProvided(@NotNull Class<?> serviceClass) {
        Objects.requireNonNull(serviceClass, "serviceClass cannot be null");
        List<ServiceRegistration<?>> list = providers.get(serviceClass);
        return list != null && !list.isEmpty();
    }

    @Override
    @NotNull
    public Set<Class<?>> getKnownServices() {
        return Collections.unmodifiableSet(providers.keySet());
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> awaitService(@NotNull Class<T> serviceClass) {
        Objects.requireNonNull(serviceClass, "serviceClass cannot be null");

        Optional<T> existing = getProvider(serviceClass);
        if (existing.isPresent()) {
            return CompletableFuture.completedFuture(existing.get());
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        synchronized (lock) {
            // Double-check inside synchronized lock
            Optional<T> doubleCheck = getProvider(serviceClass);
            if (doubleCheck.isPresent()) {
                future.complete(doubleCheck.get());
            } else {
                pendingAwaits.computeIfAbsent(serviceClass, k -> new CopyOnWriteArrayList<>())
                             .add((CompletableFuture<Object>) (CompletableFuture<?>) future);
            }
        }
        return future;
    }

    private void fireEvent(@NotNull org.bukkit.event.Event event) {
        try {
            if (Bukkit.getServer() != null && Bukkit.getPluginManager() != null) {
                Bukkit.getPluginManager().callEvent(event);
            }
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Failed to fire Bukkit event (ignorable during standalone unit tests)", t);
        }
    }
}
