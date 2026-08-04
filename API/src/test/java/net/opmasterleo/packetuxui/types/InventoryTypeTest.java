package net.opmasterleo.packetuxui.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InventoryTypeTest {

    @Test
    void hopperIsFiveSlotsNotChest() {
        assertEquals(5, InventoryType.HOPPER.size());
        assertEquals(16, InventoryType.HOPPER.id());
        assertEquals(36, InventoryType.HOPPER.bottomSlotCount());
        assertEquals(41, InventoryType.HOPPER.totalProtocolSlots());
        assertFalse(InventoryType.HOPPER.supportsChestBind());
        assertFalse(InventoryType.HOPPER.isGenericChest());
    }

    @Test
    void brewingStandHasFuelSlot() {
        assertEquals(5, InventoryType.BREWING_STAND.size());
    }

    @Test
    void lecternHasNoPlayerInventory() {
        assertEquals(1, InventoryType.LECTERN.size());
        assertEquals(0, InventoryType.LECTERN.bottomSlotCount());
        assertEquals(1, InventoryType.LECTERN.totalProtocolSlots());
    }

    @Test
    void crafterIsNineSlotGrid() {
        assertEquals(9, InventoryType.CRAFTER3X3.size());
        assertFalse(InventoryType.CRAFTER3X3.supportsChestBind());
    }

    @Test
    void genericChestsBind() {
        assertTrue(InventoryType.GENERIC9X3.supportsChestBind());
        assertTrue(InventoryType.GENERIC9X6.supportsServerBind());
        assertEquals(3, InventoryType.genericRows(3).chestRows());
        assertEquals(-1, InventoryType.HOPPER.chestRows());
    }

    @Test
    void shulkerIsPacketOnlyDespiteChestShape() {
        assertEquals(27, InventoryType.SHULKER_BOX.size());
        assertEquals(20, InventoryType.SHULKER_BOX.id());
        assertFalse(InventoryType.SHULKER_BOX.supportsChestBind());
    }
}
