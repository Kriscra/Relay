package org.vrz.relay;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.vrz.relay.api.service.ServicePriority;
import org.vrz.relay.api.service.ServiceRegistration;
import org.vrz.relay.service.RelayServiceRegistryImpl;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RelayServiceRegistryTest {

    private RelayServiceRegistryImpl registry;
    private Plugin dummyPlugin1;
    private Plugin dummyPlugin2;

    interface TestService {
        String ping();
    }

    static class NormalProvider implements TestService {
        @Override
        public String ping() { return "normal"; }
    }

    static class HighProvider implements TestService {
        @Override
        public String ping() { return "high"; }
    }

    @BeforeEach
    void setUp() {
        registry = new RelayServiceRegistryImpl();
        dummyPlugin1 = Mockito.mock(Plugin.class);
        Mockito.when(dummyPlugin1.getName()).thenReturn("Plugin1");

        dummyPlugin2 = Mockito.mock(Plugin.class);
        Mockito.when(dummyPlugin2.getName()).thenReturn("Plugin2");
    }

    @Test
    @DisplayName("Higher priority provider takes precedence over normal priority")
    void testPriorityPrecedence() {
        ServiceRegistration<TestService> regNormal = registry.register(
                TestService.class, new NormalProvider(), ServicePriority.NORMAL, dummyPlugin1
        );

        assertEquals("normal", registry.getRequiredProvider(TestService.class).ping());

        ServiceRegistration<TestService> regHigh = registry.register(
                TestService.class, new HighProvider(), ServicePriority.HIGH, dummyPlugin2
        );

        assertEquals("high", registry.getRequiredProvider(TestService.class).ping());

        // When high priority is unregistered, normal should take over again
        registry.unregister(regHigh);
        assertEquals("normal", registry.getRequiredProvider(TestService.class).ping());

        registry.unregister(regNormal);
        assertTrue(registry.getProvider(TestService.class).isEmpty());
    }

    @Test
    @DisplayName("Reactive awaitService resolves when service is registered later")
    void testAwaitService() throws Exception {
        CompletableFuture<TestService> future = registry.awaitService(TestService.class);
        assertFalse(future.isDone());

        registry.register(TestService.class, new NormalProvider(), ServicePriority.NORMAL, dummyPlugin1);

        assertTrue(future.isDone());
        TestService provider = future.get(1, TimeUnit.SECONDS);
        assertNotNull(provider);
        assertEquals("normal", provider.ping());
    }
}
