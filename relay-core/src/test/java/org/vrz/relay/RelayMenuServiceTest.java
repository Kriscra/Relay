package org.vrz.relay;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.vrz.relay.api.menu.Menu;
import org.vrz.relay.api.menu.MenuButton;
import org.vrz.relay.api.menu.PaginatedMenu;
import org.vrz.relay.menu.RelayMenuServiceImpl;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class RelayMenuServiceTest {

    private RelayMenuServiceImpl menuService;
    private Plugin testPlugin;
    private Plugin otherPlugin;
    private Player mockPlayer;
    private UUID playerUuid;

    @BeforeEach
    void setUp() {
        menuService = new RelayMenuServiceImpl();
        testPlugin = Mockito.mock(Plugin.class);
        Mockito.when(testPlugin.getName()).thenReturn("TestPlugin");

        otherPlugin = Mockito.mock(Plugin.class);
        Mockito.when(otherPlugin.getName()).thenReturn("OtherPlugin");

        mockPlayer = Mockito.mock(Player.class);
        playerUuid = UUID.randomUUID();
        Mockito.when(mockPlayer.getUniqueId()).thenReturn(playerUuid);
        Mockito.when(mockPlayer.getName()).thenReturn("Tester");

        org.vrz.relay.api.RelayProvider mockProvider = Mockito.mock(org.vrz.relay.api.RelayProvider.class);
        Mockito.when(mockProvider.getMenuService()).thenReturn(menuService);
        org.vrz.relay.api.RelayAPI.setInstance(mockProvider);
    }

    @AfterEach
    void tearDown() {
        menuService.closeAll();
        org.vrz.relay.api.RelayAPI.clearInstance();
    }

    private ItemStack createMockItem(Material material) {
        ItemStack item = Mockito.mock(ItemStack.class);
        Mockito.when(item.getType()).thenReturn(material);
        Mockito.when(item.clone()).thenReturn(item);
        return item;
    }

    @Test
    @DisplayName("MenuBuilder creates valid menu with rows, borders, and buttons")
    void testMenuBuilderLayout() {
        ItemStack borderItem = createMockItem(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack centerItem = createMockItem(Material.DIAMOND);

        AtomicBoolean clicked = new AtomicBoolean(false);

        Menu menu = menuService.createBuilder(testPlugin)
                .title(Component.text("Vault Chest"))
                .rows(3)
                .fillBorder(MenuButton.of(borderItem))
                .button(2, 5, MenuButton.of(centerItem, () -> clicked.set(true)))
                .build();

        assertNotNull(menu);
        assertEquals(3, menu.getRows());
        assertEquals(27, menu.getSize());

        // Slot (2, 5) is slot index (2-1)*9 + (5-1) = 9 + 4 = 13
        assertTrue(menu.getButton(13).isPresent());
        MenuButton centerBtn = menu.getButton(13).get();
        assertEquals(Material.DIAMOND, centerBtn.getItem(mockPlayer).getType());

        // Border slot 0 (0,0) should be border item
        assertTrue(menu.getButton(0).isPresent());
        assertEquals(Material.BLACK_STAINED_GLASS_PANE, menu.getButton(0).get().getItem(mockPlayer).getType());

        // Border slot 8 (top right)
        assertTrue(menu.getButton(8).isPresent());

        // Verify button clickability
        assertTrue(centerBtn.canClick(mockPlayer));
    }

    @Test
    @DisplayName("MenuButton debounce protects against rapid auto-click spamming")
    void testMenuButtonDebounce() {
        ItemStack item = createMockItem(Material.GOLD_INGOT);

        MenuButton button = menuService.createButtonBuilder()
                .item(item)
                .debounce(500L) // 500ms debounce
                .build();

        // 1st click should succeed
        assertTrue(button.canClick(mockPlayer));

        // Immediate 2nd click should be debounced
        assertFalse(button.canClick(mockPlayer));
    }

    @Test
    @DisplayName("MenuButton evaluates conditions and permissions correctly")
    void testMenuButtonPermissionAndCondition() {
        ItemStack item = createMockItem(Material.NETHERITE_SWORD);
        ItemStack fallback = createMockItem(Material.BARRIER);

        Mockito.when(mockPlayer.hasPermission("vip.sword")).thenReturn(false);

        MenuButton button = menuService.createButtonBuilder()
                .item(item)
                .requirePermission("vip.sword", fallback)
                .condition(p -> p.getName().equals("Tester"))
                .build();

        // Without permission, fallback barrier is displayed and cannot click
        assertEquals(Material.BARRIER, button.getItem(mockPlayer).getType());
        assertFalse(button.canClick(mockPlayer));

        // Grant permission
        Mockito.when(mockPlayer.hasPermission("vip.sword")).thenReturn(true);
        assertEquals(Material.NETHERITE_SWORD, button.getItem(mockPlayer).getType());
        assertTrue(button.canClick(mockPlayer));
    }

    @Test
    @DisplayName("PaginatedMenu handles dynamic page boundaries and pagination")
    void testPaginatedMenu() {
        List<String> entries = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            entries.add("Item #" + i);
        }

        ItemStack paperItem = createMockItem(Material.PAPER);

        // 10 slots per page -> 25 items requires 3 pages
        PaginatedMenu<String> paginated = menuService.<String>createPaginatedBuilder(testPlugin)
                .title(Component.text("Leaderboard"))
                .rows(6)
                .items(entries)
                .contentSlotRange(10, 19) // 10 slots (10 to 19 inclusive)
                .itemRenderer((name, index) -> MenuButton.of(paperItem))
                .build();

        assertNotNull(paginated);
        assertEquals(3, paginated.getMaxPages());
        assertEquals(0, paginated.getCurrentPage(mockPlayer));

        // Advance to next page
        paginated.nextPage(mockPlayer);
        assertEquals(1, paginated.getCurrentPage(mockPlayer));

        // Advance to page 2 (last page)
        paginated.nextPage(mockPlayer);
        assertEquals(2, paginated.getCurrentPage(mockPlayer));

        // Attempting to advance beyond max page remains on page 2
        paginated.nextPage(mockPlayer);
        assertEquals(2, paginated.getCurrentPage(mockPlayer));

        // Navigate back
        paginated.previousPage(mockPlayer);
        assertEquals(1, paginated.getCurrentPage(mockPlayer));
    }

    @Test
    @DisplayName("Zero-leak cleanup removes all menus owned by disabling plugin")
    void testZeroLeakPluginCleanup() {
        menuService.createBuilder(testPlugin)
                .title(Component.text("Menu 1"))
                .rows(3)
                .build();

        menuService.createBuilder(otherPlugin)
                .title(Component.text("Menu 2"))
                .rows(3)
                .build();

        assertEquals(2, menuService.getActiveMenus().size());

        // Plugin unloads
        menuService.closeAll(testPlugin);

        assertEquals(1, menuService.getActiveMenus().size());
        Menu remaining = menuService.getActiveMenus().iterator().next();
        assertEquals(otherPlugin, remaining.getPlugin());
    }
}
