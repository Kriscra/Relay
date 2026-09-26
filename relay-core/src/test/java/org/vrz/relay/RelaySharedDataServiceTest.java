package org.vrz.relay;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.vrz.relay.api.data.DataKey;
import org.vrz.relay.api.messenger.Subscription;
import org.vrz.relay.data.RelaySharedDataServiceImpl;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RelaySharedDataServiceTest {

    private RelaySharedDataServiceImpl dataService;
    private Plugin dummyPlugin;

    @BeforeEach
    void setUp() {
        dataService = new RelaySharedDataServiceImpl();
        dummyPlugin = Mockito.mock(Plugin.class);
        Mockito.when(dummyPlugin.getName()).thenReturn("TestPlugin");
    }

    @AfterEach
    void tearDown() {
        dataService.shutdown();
    }

    @Test
    @DisplayName("Set and get typed player shared data")
    void testSetAndGet() {
        UUID playerId = UUID.randomUUID();
        DataKey<Boolean> combatTag = DataKey.of("combat", "tagged", Boolean.class, false);
        DataKey<String> roleKey = DataKey.of("clan", "role", String.class);

        // Check defaults
        assertFalse(dataService.has(playerId, combatTag));
        assertFalse(dataService.getOrKeyDefault(playerId, combatTag));

        // Store value
        dataService.set(playerId, combatTag, true, dummyPlugin);
        assertTrue(dataService.has(playerId, combatTag));
        assertTrue(dataService.get(playerId, combatTag).orElse(false));

        // Store string value
        dataService.set(playerId, roleKey, "Leader", dummyPlugin);
        assertEquals("Leader", dataService.get(playerId, roleKey).orElse(null));

        // Remove
        dataService.remove(playerId, combatTag);
        assertFalse(dataService.has(playerId, combatTag));
    }

    @Test
    @DisplayName("Observe data mutations across targets")
    void testDataObservation() {
        UUID playerId = UUID.randomUUID();
        DataKey<Integer> levelKey = DataKey.of("player", "level", Integer.class, 1);

        AtomicReference<Integer> observedOld = new AtomicReference<>();
        AtomicReference<Integer> observedNew = new AtomicReference<>();

        Subscription sub = dataService.observe(levelKey, dummyPlugin, (target, key, oldVal, newVal) -> {
            observedOld.set(oldVal);
            observedNew.set(newVal);
        });

        assertTrue(sub.isActive());

        // First set (old is null, new is 5)
        dataService.set(playerId, levelKey, 5, dummyPlugin);
        assertNull(observedOld.get());
        assertEquals(5, observedNew.get());

        // Update (old is 5, new is 10)
        dataService.set(playerId, levelKey, 10, dummyPlugin);
        assertEquals(5, observedOld.get());
        assertEquals(10, observedNew.get());

        sub.unsubscribe();
        assertFalse(sub.isActive());
    }

    @Test
    @DisplayName("Plugin cleanup removes all plugin stored data and observers")
    void testPluginCleanup() {
        UUID playerId = UUID.randomUUID();
        DataKey<String> key1 = DataKey.of("mod", "data1", String.class);
        DataKey<String> key2 = DataKey.of("mod", "data2", String.class);

        dataService.set(playerId, key1, "val1", dummyPlugin);
        dataService.setGlobal(key2, "val2", dummyPlugin);

        assertTrue(dataService.has(playerId, key1));
        assertTrue(dataService.hasGlobal(key2));

        dataService.clearAll(dummyPlugin);

        assertFalse(dataService.has(playerId, key1));
        assertFalse(dataService.hasGlobal(key2));
    }
}
