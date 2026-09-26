package org.vrz.relay.api.service;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * Represents an active registration of a service provider in the Relay framework.
 *
 * @param <T> the type of service interface
 */
public interface ServiceRegistration<T> extends Comparable<ServiceRegistration<?>> {

    /**
     * Gets the contract/interface class this service satisfies.
     *
     * @return the service class token
     */
    @NotNull
    Class<T> getServiceClass();

    /**
     * Gets the concrete provider instance implementing the service interface.
     *
     * @return the provider instance
     */
    @NotNull
    T getProvider();

    /**
     * Gets the priority of this registration.
     *
     * @return the registration priority
     */
    @NotNull
    ServicePriority getPriority();

    /**
     * Gets the plugin that registered this service.
     *
     * @return the owning Bukkit plugin
     */
    @NotNull
    Plugin getPlugin();

    /**
     * Gets the timestamp when this registration occurred.
     *
     * @return instant of registration
     */
    @NotNull
    Instant getRegisteredAt();

    /**
     * Compares two registrations by priority descending, then by registration time ascending.
     *
     * @param other the other registration to compare against
     * @return negative if this is higher priority, positive if lower, 0 if equal
     */
    @Override
    default int compareTo(@NotNull ServiceRegistration<?> other) {
        int priorityComp = Integer.compare(other.getPriority().getWeight(), this.getPriority().getWeight());
        if (priorityComp != 0) {
            return priorityComp;
        }
        return this.getRegisteredAt().compareTo(other.getRegisteredAt());
    }
}
