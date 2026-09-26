package org.vrz.relay;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.vrz.relay.api.permission.PermissionService;
import org.vrz.relay.api.service.ServicePriority;
import org.vrz.relay.api.service.ServiceRegistration;
import org.vrz.relay.permission.DefaultBukkitPermissionProvider;
import org.vrz.relay.service.RelayServiceRegistryImpl;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class RelayPermissionServiceTest {

    private RelayServiceRegistryImpl registry;
    private Plugin relayPlugin;
    private Plugin luckPermsMockPlugin;

    @BeforeEach
    void setUp() {
        registry = new RelayServiceRegistryImpl();
        relayPlugin = Mockito.mock(Plugin.class);
        Mockito.when(relayPlugin.getName()).thenReturn("Relay");

        luckPermsMockPlugin = Mockito.mock(Plugin.class);
        Mockito.when(luckPermsMockPlugin.getName()).thenReturn("LuckPerms");
    }

    @Test
    @DisplayName("Fallback Bukkit permissions work and seamlessly transition when LuckPerms registers")
    void testPermissionFallbackAndOverride() throws Exception {
        DefaultBukkitPermissionProvider fallbackProvider = new DefaultBukkitPermissionProvider();

        // 1. Relay registers fallback provider
        registry.register(PermissionService.class, fallbackProvider, ServicePriority.FALLBACK, relayPlugin);

        PermissionService current = registry.getRequiredProvider(PermissionService.class);
        assertSame(fallbackProvider, current);

        UUID testUser = UUID.randomUUID();
        assertEquals("default", current.getPrimaryGroup(testUser).get());

        // 2. Mock advanced provider (e.g. LuckPerms) registers with HIGHEST priority
        PermissionService advancedProvider = Mockito.mock(PermissionService.class);
        Mockito.when(advancedProvider.getPrimaryGroup(testUser)).thenReturn(CompletableFuture.completedFuture("administrator"));

        ServiceRegistration<PermissionService> luckPermsReg = registry.register(
                PermissionService.class, advancedProvider, ServicePriority.HIGHEST, luckPermsMockPlugin
        );

        // Registry should immediately switch to advancedProvider
        PermissionService newActive = registry.getRequiredProvider(PermissionService.class);
        assertSame(advancedProvider, newActive);
        assertEquals("administrator", newActive.getPrimaryGroup(testUser).get());

        // 3. When LuckPerms unregisters or server reloads, falls back to default provider seamlessly
        registry.unregister(luckPermsReg);
        PermissionService fallbackActive = registry.getRequiredProvider(PermissionService.class);
        assertSame(fallbackProvider, fallbackActive);
        assertEquals("default", fallbackActive.getPrimaryGroup(testUser).get());
    }
}
