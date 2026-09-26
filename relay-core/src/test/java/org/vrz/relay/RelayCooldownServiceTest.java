package org.vrz.relay;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.vrz.relay.api.cooldown.CooldownEntry;
import org.vrz.relay.cooldown.RelayCooldownServiceImpl;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RelayCooldownServiceTest {

    private RelayCooldownServiceImpl cooldownService;
    private Plugin dummyPlugin;

    @BeforeEach
    void setUp() {
        cooldownService = new RelayCooldownServiceImpl();
        dummyPlugin = Mockito.mock(Plugin.class);
        Mockito.when(dummyPlugin.getName()).thenReturn("TestPlugin");
    }

    @AfterEach
    void tearDown() {
        cooldownService.shutdown();
    }

    @Test
    @DisplayName("Player cooldown is correctly set, queried and formatted")
    void testSetAndQueryCooldown() {
        UUID playerId = UUID.randomUUID();
        String key = "ability:dash";

        assertFalse(cooldownService.hasCooldown(playerId, key));

        CooldownEntry entry = cooldownService.setCooldown(playerId, key, Duration.ofSeconds(10), dummyPlugin);
        assertNotNull(entry);
        assertTrue(cooldownService.hasCooldown(playerId, key));

        Duration remaining = cooldownService.getRemaining(playerId, key);
        assertTrue(remaining.toMillis() > 0 && remaining.toMillis() <= 10000);

        String formatted = cooldownService.formatRemaining(playerId, key);
        assertTrue(formatted.endsWith("s"));

        // Progress should be between 0.0 and 1.0
        double progress = entry.getProgress();
        assertTrue(progress >= 0.0 && progress <= 1.0);

        // Clear cooldown
        boolean cleared = cooldownService.clearCooldown(playerId, key);
        assertTrue(cleared);
        assertFalse(cooldownService.hasCooldown(playerId, key));
    }

    @Test
    @DisplayName("Global cooldown is correctly managed")
    void testGlobalCooldown() {
        String key = "event:boss_spawn";

        assertFalse(cooldownService.hasGlobalCooldown(key));

        cooldownService.setGlobalCooldown(key, Duration.ofMinutes(5), dummyPlugin);
        assertTrue(cooldownService.hasGlobalCooldown(key));

        Duration rem = cooldownService.getGlobalRemaining(key);
        assertTrue(rem.toSeconds() > 0);

        cooldownService.clearGlobalCooldown(key);
        assertFalse(cooldownService.hasGlobalCooldown(key));
    }

    @Test
    @DisplayName("Clearing plugin cooldowns removes registered cooldowns")
    void testPluginCleanup() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        cooldownService.setCooldown(player1, "action:1", Duration.ofMinutes(1), dummyPlugin);
        cooldownService.setCooldown(player2, "action:2", Duration.ofMinutes(1), dummyPlugin);

        assertTrue(cooldownService.hasCooldown(player1, "action:1"));
        assertTrue(cooldownService.hasCooldown(player2, "action:2"));

        cooldownService.clearAllCooldowns(dummyPlugin);

        assertFalse(cooldownService.hasCooldown(player1, "action:1"));
        assertFalse(cooldownService.hasCooldown(player2, "action:2"));
    }
}
