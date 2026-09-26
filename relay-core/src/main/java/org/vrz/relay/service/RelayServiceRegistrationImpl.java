package org.vrz.relay.service;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.service.ServicePriority;
import org.vrz.relay.api.service.ServiceRegistration;

import java.time.Instant;
import java.util.Objects;

/**
 * Concrete implementation of {@link ServiceRegistration}.
 *
 * @param <T> service type
 */
public final class RelayServiceRegistrationImpl<T> implements ServiceRegistration<T> {

    private final Class<T> serviceClass;
    private final T provider;
    private final ServicePriority priority;
    private final Plugin plugin;
    private final Instant registeredAt;

    public RelayServiceRegistrationImpl(@NotNull Class<T> serviceClass,
                                        @NotNull T provider,
                                        @NotNull ServicePriority priority,
                                        @NotNull Plugin plugin) {
        this.serviceClass = Objects.requireNonNull(serviceClass, "serviceClass cannot be null");
        this.provider = Objects.requireNonNull(provider, "provider cannot be null");
        this.priority = Objects.requireNonNull(priority, "priority cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.registeredAt = Instant.now();
    }

    @Override
    @NotNull
    public Class<T> getServiceClass() {
        return serviceClass;
    }

    @Override
    @NotNull
    public T getProvider() {
        return provider;
    }

    @Override
    @NotNull
    public ServicePriority getPriority() {
        return priority;
    }

    @Override
    @NotNull
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    @NotNull
    public Instant getRegisteredAt() {
        return registeredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceRegistration<?> that)) return false;
        return Objects.equals(serviceClass, that.getServiceClass()) &&
               Objects.equals(provider, that.getProvider()) &&
               Objects.equals(plugin, that.getPlugin());
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceClass, provider, plugin);
    }

    @Override
    public String toString() {
        return "ServiceRegistration{" +
                "service=" + serviceClass.getSimpleName() +
                ", provider=" + provider.getClass().getSimpleName() +
                ", priority=" + priority +
                ", plugin=" + plugin.getName() +
                '}';
    }
}
