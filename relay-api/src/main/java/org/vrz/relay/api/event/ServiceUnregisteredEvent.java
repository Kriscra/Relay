package org.vrz.relay.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.service.ServiceRegistration;

/**
 * Called when a service provider registration is removed from the Relay registry.
 *
 * @param <T> the service type
 */
public class ServiceUnregisteredEvent<T> extends RelayEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ServiceRegistration<T> registration;
    private final ServiceRegistration<T> newPrimary;

    public ServiceUnregisteredEvent(@NotNull ServiceRegistration<T> registration,
                                   @Nullable ServiceRegistration<T> newPrimary) {
        super(true);
        this.registration = registration;
        this.newPrimary = newPrimary;
    }

    @NotNull
    public ServiceRegistration<T> getRegistration() {
        return registration;
    }

    @NotNull
    public Class<T> getServiceClass() {
        return registration.getServiceClass();
    }

    @NotNull
    public T getProvider() {
        return registration.getProvider();
    }

    /**
     * Gets the new primary provider that took over, or null if no provider remains.
     *
     * @return the new primary registration or null
     */
    @Nullable
    public ServiceRegistration<T> getNewPrimary() {
        return newPrimary;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
