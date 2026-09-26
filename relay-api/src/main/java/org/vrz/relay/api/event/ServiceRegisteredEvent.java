package org.vrz.relay.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.service.ServiceRegistration;

/**
 * Called when a new service provider registration is added to the Relay {@link org.vrz.relay.api.service.ServiceRegistry}.
 *
 * @param <T> the service type
 */
public class ServiceRegisteredEvent<T> extends RelayEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ServiceRegistration<T> registration;
    private final boolean becamePrimary;

    public ServiceRegisteredEvent(@NotNull ServiceRegistration<T> registration, boolean becamePrimary) {
        super(true); // Dispatched asynchronously or synchronously depending on caller
        this.registration = registration;
        this.becamePrimary = becamePrimary;
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
     * Checks if this registration became the highest-priority active provider.
     *
     * @return true if this registration is now the primary active provider
     */
    public boolean becamePrimary() {
        return becamePrimary;
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
