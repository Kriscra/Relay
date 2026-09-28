package org.vrz.relay;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.vrz.relay.api.notification.*;
import org.vrz.relay.notification.RelayNotificationServiceImpl;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class RelayNotificationServiceTest {

    private RelayNotificationServiceImpl notificationService;
    private Plugin dummyPluginA;
    private Plugin dummyPluginB;
    private Player dummyPlayer;
    private UUID playerUuid;

    @BeforeEach
    void setUp() {
        notificationService = new RelayNotificationServiceImpl();

        dummyPluginA = Mockito.mock(Plugin.class);
        Mockito.when(dummyPluginA.getName()).thenReturn("PluginA");

        dummyPluginB = Mockito.mock(Plugin.class);
        Mockito.when(dummyPluginB.getName()).thenReturn("PluginB");

        dummyPlayer = Mockito.mock(Player.class);
        playerUuid = UUID.randomUUID();
        Mockito.when(dummyPlayer.getUniqueId()).thenReturn(playerUuid);
        Mockito.when(dummyPlayer.getName()).thenReturn("TestHero");
        Mockito.when(dummyPlayer.isOnline()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        notificationService.shutdown();
    }

    @Test
    @DisplayName("ActionBar preemption: HIGH priority preempts NORMAL priority")
    void testActionBarPreemption() {
        // Enqueue NORMAL notification
        ActionBarNotification normal = notificationService.actionBar(dummyPlayer)
                .message(Component.text("Normal Message"))
                .priority(NotificationPriority.NORMAL)
                .duration(Duration.ofSeconds(10))
                .send(dummyPluginA);

        Optional<ActionBarNotification> active1 = notificationService.getActiveActionBar(dummyPlayer);
        assertTrue(active1.isPresent());
        assertEquals("Normal Message", ((net.kyori.adventure.text.TextComponent) active1.get().getMessage()).content());

        // Enqueue HIGH priority notification -> should preempt NORMAL
        ActionBarNotification high = notificationService.actionBar(dummyPlayer)
                .message(Component.text("CRITICAL ALERT!"))
                .priority(NotificationPriority.HIGH)
                .duration(Duration.ofSeconds(5))
                .send(dummyPluginB);

        Optional<ActionBarNotification> active2 = notificationService.getActiveActionBar(dummyPlayer);
        assertTrue(active2.isPresent());
        assertEquals("CRITICAL ALERT!", ((net.kyori.adventure.text.TextComponent) active2.get().getMessage()).content());
    }

    @Test
    @DisplayName("Continuous ActionBar fallback: activates when no notifications are queued")
    void testContinuousActionBar() {
        AtomicBoolean supplied = new AtomicBoolean(false);

        notificationService.registerContinuousActionBar(dummyPluginA, dummyPlayer, p -> {
            supplied.set(true);
            return Component.text("Mana: 100/100");
        });

        // Tick with no active notification
        notificationService.tick();
        assertTrue(supplied.get());
        Mockito.verify(dummyPlayer, Mockito.atLeastOnce()).sendActionBar(Mockito.any(Component.class));
    }

    @Test
    @DisplayName("BossBar countdown, progress calculation, and completion callback")
    void testBossBarCountdown() {
        AtomicBoolean completed = new AtomicBoolean(false);

        ActiveBossBar bar = notificationService.bossBar(dummyPlayer)
                .titleMiniMessage("<aqua>Dungeon Timer</aqua>")
                .countdown(Duration.ofMillis(200))
                .color(BossBar.Color.RED)
                .onComplete(() -> completed.set(true))
                .send(dummyPluginA);

        assertNotNull(bar);
        assertEquals(1.0f, bar.getProgress(), 0.01f);
        assertTrue(bar.isVisible());

        Collection<ActiveBossBar> bars = notificationService.getActiveBossBars(dummyPlayer);
        assertEquals(1, bars.size());

        // Sleep to let duration pass
        try {
            Thread.sleep(250);
        } catch (InterruptedException ignored) {}

        notificationService.tick();

        assertTrue(completed.get(), "BossBar completion callback should have triggered");
        assertFalse(bar.isVisible(), "BossBar should be dismissed");
        assertEquals(0, notificationService.getActiveBossBars(dummyPlayer).size());
    }

    @Test
    @DisplayName("Toast builder creates valid notification with frame and icon")
    void testToastBuilder() {
        ToastNotification toast = notificationService.toast(dummyPlayer)
                .title(Component.text("Quest Completed"))
                .description(Component.text("Slay 5 dragons"))
                .icon(Material.DIAMOND)
                .frame(ToastFrame.CHALLENGE)
                .send(dummyPluginA);

        assertNotNull(toast);
        assertEquals(Material.DIAMOND, toast.getIcon());
        assertEquals(ToastFrame.CHALLENGE, toast.getFrame());
        assertEquals("Quest Completed", ((net.kyori.adventure.text.TextComponent) toast.getTitle()).content());
    }

    @Test
    @DisplayName("Zero-leak: Player quit immediately clears active notifications and bossbars")
    void testPlayerQuitCleanup() {
        notificationService.actionBar(dummyPlayer)
                .message(Component.text("Persistent"))
                .duration(Duration.ofSeconds(30))
                .send(dummyPluginA);

        notificationService.bossBar(dummyPlayer)
                .title(Component.text("Boss"))
                .countdown(Duration.ofSeconds(60))
                .send(dummyPluginA);

        assertTrue(notificationService.getActiveActionBar(dummyPlayer).isPresent());
        assertEquals(1, notificationService.getActiveBossBars(dummyPlayer).size());

        // Player quits
        notificationService.handlePlayerQuit(dummyPlayer);

        assertFalse(notificationService.getActiveActionBar(dummyPlayer).isPresent());
        assertEquals(0, notificationService.getActiveBossBars(dummyPlayer).size());
    }

    @Test
    @DisplayName("Zero-leak: Plugin disable cleans only that plugin's notifications and bossbars")
    void testPluginDisableCleanup() {
        // Plugin A creates an actionbar and a bossbar
        notificationService.actionBar(dummyPlayer)
                .message(Component.text("From Plugin A"))
                .duration(Duration.ofSeconds(30))
                .send(dummyPluginA);

        ActiveBossBar barA = notificationService.bossBar(dummyPlayer)
                .title(Component.text("Bar A"))
                .countdown(Duration.ofSeconds(60))
                .send(dummyPluginA);

        // Plugin B creates a bossbar
        ActiveBossBar barB = notificationService.bossBar(dummyPlayer)
                .title(Component.text("Bar B"))
                .countdown(Duration.ofSeconds(60))
                .send(dummyPluginB);

        assertEquals(2, notificationService.getActiveBossBars(dummyPlayer).size());

        // Disable Plugin A
        notificationService.clearAll(dummyPluginA);

        // Bar A should be dismissed, Bar B should remain
        assertFalse(barA.isVisible());
        assertTrue(barB.isVisible());
        assertEquals(1, notificationService.getActiveBossBars(dummyPlayer).size());
    }
}
