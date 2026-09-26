package org.vrz.relay.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.service.ServiceRegistration;

/**
 * Called when the primary active provider for a service changes (due to a higher priority provider
 * registering, or the previous active provider unregistering).
 *
 * @param <T> the service type
 */
public class PrimaryProviderChangedEvent<T> extends RelayEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Class<T> serviceClass;
    private final ServiceRegistration<T> previousPrimary;
    private final ServiceRegistration<T> newPrimary;

    public PrimaryProviderChangedEvent(@NotNull Class<T> serviceClass,
                                       @Nullable ServiceRegistration<T> previousPrimary,
                                       @Nullable ServiceRegistration<T> newPrimary) {
        super(true);
        this.serviceClass = serviceClass;
        this.previousPrimary = previousPrimary;
        this.newPrimary = newPrimary;
    }

    @NotNull
    public Class<T> getServiceClass() {
        return serviceClass;
    }

    @Nullable
    public ServiceRegistration<T> getPreviousPrimary() {
        return previousPrimary;
    }

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
