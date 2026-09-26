package org.vrz.relay;

import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.RelayProvider;
import org.vrz.relay.api.cooldown.CooldownService;
import org.vrz.relay.api.data.SharedDataService;
import org.vrz.relay.api.messenger.RelayMessenger;
import org.vrz.relay.api.service.ServiceRegistry;

import java.util.Objects;

/**
 * Default implementation of {@link RelayProvider} bound to the Relay runtime.
 */
public final class RelayProviderImpl implements RelayProvider {

    private final ServiceRegistry serviceRegistry;
    private final RelayMessenger messenger;
    private final CooldownService cooldownService;
    private final SharedDataService dataService;

    public RelayProviderImpl(@NotNull ServiceRegistry serviceRegistry,
                             @NotNull RelayMessenger messenger,
                             @NotNull CooldownService cooldownService,
                             @NotNull SharedDataService dataService) {
        this.serviceRegistry = Objects.requireNonNull(serviceRegistry, "serviceRegistry cannot be null");
        this.messenger = Objects.requireNonNull(messenger, "messenger cannot be null");
        this.cooldownService = Objects.requireNonNull(cooldownService, "cooldownService cannot be null");
        this.dataService = Objects.requireNonNull(dataService, "dataService cannot be null");
    }

    @Override
    @NotNull
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    @Override
    @NotNull
    public RelayMessenger getMessenger() {
        return messenger;
    }

    @Override
    @NotNull
    public CooldownService getCooldownService() {
        return cooldownService;
    }

    @Override
    @NotNull
    public SharedDataService getDataService() {
        return dataService;
    }
}
