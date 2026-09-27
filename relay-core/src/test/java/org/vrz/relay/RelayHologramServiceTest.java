package org.vrz.relay;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.vrz.relay.api.hologram.Hologram;
import org.vrz.relay.api.hologram.HologramLine;
import org.vrz.relay.hologram.RelayHologramServiceImpl;

import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RelayHologramServiceTest {

    private RelayHologramServiceImpl hologramService;
    private Plugin testPlugin;
    private Plugin otherPlugin;
    private Location dummyLocation;

    @BeforeEach
    void setUp() {
        hologramService = new RelayHologramServiceImpl();
        testPlugin = Mockito.mock(Plugin.class);
        Mockito.when(testPlugin.getName()).thenReturn("TestPlugin");

        otherPlugin = Mockito.mock(Plugin.class);
        Mockito.when(otherPlugin.getName()).thenReturn("OtherPlugin");

        World dummyWorld = Mockito.mock(World.class);
        Mockito.when(dummyWorld.getName()).thenReturn("world");
        dummyLocation = new Location(dummyWorld, 100, 64, 100);
    }

    @AfterEach
    void tearDown() {
        hologramService.deleteAll();
    }

    @Test
    @DisplayName("Create hologram and manage text lines correctly")
    void testCreateHologramAndLines() {
        Hologram holo = hologramService.createHologram("spawn_welcome", dummyLocation, testPlugin);
        assertNotNull(holo);
        assertEquals("spawn_welcome", holo.getId());
        assertEquals(0, holo.getLineCount());

        HologramLine line1 = holo.appendLine(Component.text("Welcome to Server!"));
        assertEquals(0, line1.getIndex());
        assertEquals(1, holo.getLineCount());

        HologramLine line2 = holo.appendLine("<gold>Line 2 MiniMessage</gold>");
        assertEquals(1, line2.getIndex());
        assertEquals(2, holo.getLineCount());

        holo.insertLine(1, Component.text("Inserted Line"));
        assertEquals(3, holo.getLineCount());
        assertEquals(0, holo.getLine(0).getIndex());
        assertEquals(1, holo.getLine(1).getIndex());
        assertEquals(2, holo.getLine(2).getIndex());

        holo.removeLine(1);
        assertEquals(2, holo.getLineCount());
        assertEquals(1, holo.getLine(1).getIndex());

        holo.clearLines();
        assertEquals(0, holo.getLineCount());
    }

    @Test
    @DisplayName("Fluent HologramBuilder builds and configures all parameters")
    void testFluentBuilder() {
        Hologram holo = hologramService.createBuilder("npc_leaderboard")
                .location(dummyLocation)
                .owner(testPlugin)
                .addLine("<yellow>=== TOP PLAYERS ===</yellow>")
                .addLine("<gray>#1 Notch - 1000 Kills</gray>")
                .billboard(Display.Billboard.VERTICAL)
                .shadow(true)
                .seeThrough(true)
                .backgroundColor(Color.fromRGB(20, 20, 20))
                .lineSpacing(0.35)
                .build();

        assertNotNull(holo);
        assertEquals("npc_leaderboard", holo.getId());
        assertEquals(2, holo.getLineCount());
        assertEquals(Display.Billboard.VERTICAL, holo.getBillboard());
        assertTrue(holo.hasTextShadow());
        assertTrue(holo.isSeeThrough());
        assertEquals(Color.fromRGB(20, 20, 20), holo.getBackgroundColor());
        assertEquals(0.35, holo.getLineSpacing(), 0.001);

        Optional<Hologram> queried = hologramService.getHologram("npc_leaderboard");
        assertTrue(queried.isPresent());
        assertSame(holo, queried.get());
    }

    @Test
    @DisplayName("Plugin-specific deletion and zero-leak cleanup")
    void testPluginSpecificCleanup() {
        hologramService.createHologram("holo1", dummyLocation, testPlugin);
        hologramService.createHologram("holo2", dummyLocation, testPlugin);
        hologramService.createHologram("holo3", dummyLocation, otherPlugin);

        assertEquals(3, hologramService.getActiveHologramCount());

        Collection<Hologram> testHols = hologramService.getHologramsByPlugin(testPlugin);
        assertEquals(2, testHols.size());

        // Zero-leak simulation: testPlugin disables
        hologramService.deleteAll(testPlugin);
        assertEquals(1, hologramService.getActiveHologramCount());
        assertTrue(hologramService.getHologram("holo1").isEmpty());
        assertTrue(hologramService.getHologram("holo2").isEmpty());
        assertTrue(hologramService.getHologram("holo3").isPresent());

        // Delete remaining
        assertTrue(hologramService.deleteHologram("holo3"));
        assertEquals(0, hologramService.getActiveHologramCount());
    }

    @Test
    @DisplayName("Line vertical offsets compute correctly based on spacing")
    void testVerticalOffsets() {
        Hologram holo = hologramService.createHologram("offset_test", dummyLocation, testPlugin);
        holo.setLineSpacing(0.30);

        holo.appendLine("Top Line");
        holo.appendLine("Middle Line");
        holo.appendLine("Bottom Line");

        // Total lines = 3: index 0 is top (offset = 2 * 0.30 = 0.60), index 2 is bottom (offset = 0.0)
        assertEquals(0.60, holo.getLine(0).getVerticalOffset(), 0.001);
        assertEquals(0.30, holo.getLine(1).getVerticalOffset(), 0.001);
        assertEquals(0.00, holo.getLine(2).getVerticalOffset(), 0.001);

        // Custom override
        holo.getLine(0).setVerticalOffset(1.5);
        assertEquals(1.5, holo.getLine(0).getVerticalOffset(), 0.001);
    }
}
