package org.vrz.relay;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.vrz.relay.api.scoreboard.Sidebar;
import org.vrz.relay.scoreboard.RelayScoreboardServiceImpl;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class RelayScoreboardServiceTest {

    private RelayScoreboardServiceImpl scoreboardService;
    private Plugin testPlugin;
    private Plugin otherPlugin;
    private Player mockPlayer;
    private UUID playerUuid;

    @BeforeEach
    void setUp() {
        scoreboardService = new RelayScoreboardServiceImpl(null);

        testPlugin = Mockito.mock(Plugin.class);
        when(testPlugin.getName()).thenReturn("TestPlugin");

        otherPlugin = Mockito.mock(Plugin.class);
        when(otherPlugin.getName()).thenReturn("OtherPlugin");

        playerUuid = UUID.randomUUID();
        mockPlayer = Mockito.mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerUuid);
        when(mockPlayer.getName()).thenReturn("Tester");
        when(mockPlayer.isOnline()).thenReturn(true);
    }

    @Test
    @DisplayName("SidebarBuilder creates sidebar with static lines and MiniMessage title")
    void testSidebarCreationAndLines() {
        Sidebar sidebar = scoreboardService.sidebarBuilder(testPlugin)
                .titleMiniMessage("<gold><bold>SERVER STATS</bold></gold>")
                .lineMiniMessage(1, "<gray>Online: <green>50</gray>")
                .line(2, Component.text("Ping: 25ms"))
                .lineMiniMessage(3, "<aqua>Rank: VIP</aqua>")
                .buildAndShow(mockPlayer);

        assertNotNull(sidebar);
        assertEquals(testPlugin, sidebar.getPlugin());
        assertEquals(mockPlayer, sidebar.getPlayer());
        assertFalse(sidebar.isDestroyed());

        assertEquals(3, sidebar.getLines().size());
        assertEquals("Online: 50", PlainTextComponentSerializer.plainText().serialize(sidebar.getLine(1)));
        assertEquals("Ping: 25ms", PlainTextComponentSerializer.plainText().serialize(sidebar.getLine(2)));
        assertEquals("Rank: VIP", PlainTextComponentSerializer.plainText().serialize(sidebar.getLine(3)));

        assertTrue(scoreboardService.getSidebar(mockPlayer).isPresent());
        assertEquals(sidebar, scoreboardService.getSidebar(mockPlayer).get());
        assertEquals(1, scoreboardService.getActiveSidebarCount());
    }

    @Test
    @DisplayName("Dynamic line suppliers re-evaluate properly upon update()")
    void testDynamicLineSuppliers() {
        AtomicInteger coinCounter = new AtomicInteger(100);

        Sidebar sidebar = scoreboardService.sidebarBuilder(testPlugin)
                .title(Component.text("Economy HUD"))
                .line(1, () -> Component.text("Coins: " + coinCounter.get()))
                .buildAndShow(mockPlayer);

        assertEquals("Coins: 100", PlainTextComponentSerializer.plainText().serialize(sidebar.getLine(1)));

        // Increment coins and call update
        coinCounter.set(250);
        sidebar.update();

        assertEquals("Coins: 250", PlainTextComponentSerializer.plainText().serialize(sidebar.getLine(1)));
    }

    @Test
    @DisplayName("Sidebar lines sequentially set via list and MiniMessage")
    void testSequentialLines() {
        Sidebar sidebar = scoreboardService.sidebarBuilder(testPlugin)
                .title(Component.text("Lines Test"))
                .linesMiniMessage(List.of(
                        "<red>First Line</red>",
                        "<green>Second Line</green>",
                        "<blue>Third Line</blue>"
                ))
                .build();

        assertEquals(3, sidebar.getLines().size());
        assertEquals("First Line", PlainTextComponentSerializer.plainText().serialize(sidebar.getLine(1)));
        assertEquals("Second Line", PlainTextComponentSerializer.plainText().serialize(sidebar.getLine(2)));
        assertEquals("Third Line", PlainTextComponentSerializer.plainText().serialize(sidebar.getLine(3)));

        sidebar.removeLine(2);
        assertNull(sidebar.getLine(2));
        assertEquals(2, sidebar.getLines().size());

        sidebar.clearLines();
        assertEquals(0, sidebar.getLines().size());
    }

    @Test
    @DisplayName("Priority stacking: Higher priority sidebar takes precedence and falls back when destroyed")
    void testPriorityStacking() {
        Sidebar defaultSidebar = scoreboardService.sidebarBuilder(testPlugin)
                .title(Component.text("Default Sidebar"))
                .priority(0)
                .line(1, Component.text("Normal Mode"))
                .buildAndShow(mockPlayer);

        assertEquals(defaultSidebar, scoreboardService.getSidebar(mockPlayer).get());

        // Combat plugin opens high priority sidebar (e.g. priority 10)
        Sidebar combatSidebar = scoreboardService.sidebarBuilder(otherPlugin)
                .title(Component.text("Combat Tag"))
                .priority(10)
                .line(1, Component.text("In Combat: 15s"))
                .buildAndShow(mockPlayer);

        // Highest priority is now combatSidebar
        assertEquals(combatSidebar, scoreboardService.getSidebar(mockPlayer).get());
        assertEquals(2, scoreboardService.getActiveSidebarCount());

        // Combat finishes, sidebar destroyed -> falls back to defaultSidebar
        combatSidebar.destroy();
        assertEquals(defaultSidebar, scoreboardService.getSidebar(mockPlayer).get());
        assertEquals(1, scoreboardService.getActiveSidebarCount());
    }

    @Test
    @DisplayName("Zero-leak: clearAll(plugin) destroys only the target plugin's sidebars")
    void testZeroLeakPluginCleanup() {
        Sidebar sb1 = scoreboardService.sidebarBuilder(testPlugin)
                .title(Component.text("Plugin A"))
                .buildAndShow(mockPlayer);

        Player player2 = Mockito.mock(Player.class);
        UUID p2Uuid = UUID.randomUUID();
        when(player2.getUniqueId()).thenReturn(p2Uuid);
        when(player2.getName()).thenReturn("OtherUser");
        when(player2.isOnline()).thenReturn(true);

        Sidebar sb2 = scoreboardService.sidebarBuilder(otherPlugin)
                .title(Component.text("Plugin B"))
                .buildAndShow(player2);

        assertEquals(2, scoreboardService.getActiveSidebarCount());

        // Plugin A is disabled
        scoreboardService.clearAll(testPlugin);

        assertTrue(sb1.isDestroyed());
        assertFalse(sb2.isDestroyed());
        assertEquals(1, scoreboardService.getActiveSidebarCount());
        assertFalse(scoreboardService.getSidebar(mockPlayer).isPresent());
        assertTrue(scoreboardService.getSidebar(player2).isPresent());
    }

    @Test
    @DisplayName("Player quit automatically cleans up sidebars and frees references")
    void testPlayerQuitCleanup() {
        Sidebar sidebar = scoreboardService.sidebarBuilder(testPlugin)
                .title(Component.text("Player Quit Test"))
                .buildAndShow(mockPlayer);

        assertEquals(1, scoreboardService.getActiveSidebarCount());
        assertTrue(scoreboardService.getSidebar(mockPlayer).isPresent());

        // Player quits
        PlayerQuitEvent quitEvent = new PlayerQuitEvent(mockPlayer, Component.text("Quit"));
        scoreboardService.onPlayerQuit(quitEvent);

        assertTrue(sidebar.isDestroyed());
        assertEquals(0, scoreboardService.getActiveSidebarCount());
        assertFalse(scoreboardService.getSidebar(mockPlayer).isPresent());
    }

    @Test
    @DisplayName("Animated title frames configuration")
    void testAnimatedTitle() {
        Sidebar sidebar = scoreboardService.sidebarBuilder(testPlugin)
                .animatedTitle(List.of(
                        Component.text("Frame 1"),
                        Component.text("Frame 2"),
                        Component.text("Frame 3")
                ), 10L)
                .build();

        assertEquals("Frame 1", PlainTextComponentSerializer.plainText().serialize(sidebar.getTitle()));
    }
}
