package net.opmasterleo.packetuxui.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import net.opmasterleo.packetuxui.service.GuiScopeListener;
import net.opmasterleo.packetuxui.service.MenuMode;
import net.opmasterleo.packetuxui.types.InventoryType;

class MenuBuildTest {

    @Test
    void rowsMapToInventoryType() {
        assertEquals(InventoryType.GENERIC9X1, MenuBuild.create().rows(1).type());
        assertEquals(InventoryType.GENERIC9X6, MenuBuild.create().rows(6).type());
        assertEquals(MenuMode.EDITABLE, MenuBuild.create().editable().mode());
        assertEquals(MenuMode.READ_ONLY, MenuBuild.create().readOnly().mode());
    }

    @Test
    void typedHelpersSelectCorrectInventoryType() {
        assertEquals(InventoryType.HOPPER, MenuBuild.create().hopper().type());
        assertEquals(InventoryType.ANVIL, MenuBuild.create().anvil().type());
        assertEquals(InventoryType.FURNACE, MenuBuild.create().furnace().type());
        assertEquals(InventoryType.SMOKER, MenuBuild.create().smoker().type());
        assertEquals(InventoryType.BLAST_FURNACE, MenuBuild.create().blastFurnace().type());
        assertEquals(InventoryType.BREWING_STAND, MenuBuild.create().brewingStand().type());
        assertEquals(InventoryType.GRINDSTONE, MenuBuild.create().grindstone().type());
        assertEquals(InventoryType.SMITHING_TABLE, MenuBuild.create().smithingTable().type());
        assertEquals(InventoryType.LOOM, MenuBuild.create().loom().type());
        assertEquals(InventoryType.CARTOGRAPHY_TABLE, MenuBuild.create().cartographyTable().type());
        assertEquals(InventoryType.STONECUTTER, MenuBuild.create().stonecutter().type());
        assertEquals(InventoryType.BEACON, MenuBuild.create().beacon().type());
        assertEquals(InventoryType.ENCHANTMENT_TABLE, MenuBuild.create().enchantmentTable().type());
        assertEquals(InventoryType.CRAFTING_TABLE, MenuBuild.create().craftingTable().type());
        assertEquals(InventoryType.GENERIC3X3, MenuBuild.create().dispenser().type());
        assertEquals(InventoryType.SHULKER_BOX, MenuBuild.create().shulkerBox().type());
        assertEquals(InventoryType.VILLAGER, MenuBuild.create().merchant().type());
        assertEquals(InventoryType.LECTERN, MenuBuild.create().lectern().type());
        assertEquals(InventoryType.CRAFTER3X3, MenuBuild.create().crafter().type());
    }

    @Test
    void typeOverridesRows() {
        MenuBuild hopper = MenuBuild.create().rows(6).type(InventoryType.HOPPER);
        assertEquals(InventoryType.HOPPER, hopper.type());
        assertEquals(5, hopper.type().size());
        assertEquals(-1, hopper.rows());
        assertFalse(hopper.type().supportsChestBind());
    }

    @Test
    void scopeListenerOrderingContract() {
        List<String> events = new ArrayList<>();
        AtomicInteger top = new AtomicInteger();
        GuiScopeListener listener = (player, open, topSlotCount) -> {
            events.add(open ? "open:" + topSlotCount : "close:" + topSlotCount);
            top.set(topSlotCount);
        };
        listener.onScopeChanged(null, true, 27);
        listener.onScopeChanged(null, false, 27);
        assertEquals(List.of("open:27", "close:27"), events);
        assertEquals(27, top.get());
    }
}
