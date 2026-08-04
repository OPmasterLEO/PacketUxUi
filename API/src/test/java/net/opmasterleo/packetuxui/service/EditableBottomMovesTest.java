package net.opmasterleo.packetuxui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.opmasterleo.packetuxui.nms.item.UxItem;

class EditableBottomMovesTest {

    @Test
    void hotbarPickupThenBackpackPlace() {
        List<UxItem> bottom = emptyBottom();
        bottom.set(27, stack("minecraft:diamond", 1));
        EditableBottomMoves.Outcome picked = EditableBottomMoves.applyPickup(bottom, null, 27, 0);
        assertNotNull(picked.held());
        assertEquals(27, picked.held().originIndex());
        assertTrue(picked.bottom().get(27).isEmpty());
        assertEquals(1, picked.held().item().amount());

        EditableBottomMoves.Outcome placed = EditableBottomMoves.applyPickup(
                picked.bottom(), picked.held(), 0, 0
        );
        assertNull(placed.held());
        assertEquals(1, placed.bottom().get(0).amount());
        assertTrue(placed.bottom().get(27).isEmpty());
        assertConserved(bottom, UxItem.EMPTY, placed.bottom(), UxItem.EMPTY);
    }

    @Test
    void rightClickSplitThenPlace() {
        List<UxItem> bottom = emptyBottom();
        bottom.set(3, stack("minecraft:cobblestone", 5));
        EditableBottomMoves.Outcome split = EditableBottomMoves.applyPickup(bottom, null, 3, 1);
        assertEquals(2, split.bottom().get(3).amount());
        assertEquals(3, split.held().item().amount());
        assertEquals(3, split.held().originIndex());

        EditableBottomMoves.Outcome placed = EditableBottomMoves.applyPickup(
                split.bottom(), split.held(), 10, 0
        );
        assertNull(placed.held());
        assertEquals(3, placed.bottom().get(10).amount());
        assertEquals(2, placed.bottom().get(3).amount());
        assertConserved(bottom, UxItem.EMPTY, placed.bottom(), UxItem.EMPTY);
    }

    @Test
    void returnToOriginRestoresPickupSlot() {
        List<UxItem> bottom = emptyBottom();
        bottom.set(5, stack("minecraft:apple", 2));
        EditableBottomMoves.Outcome picked = EditableBottomMoves.applyPickup(bottom, null, 5, 0);
        List<UxItem> restored = EditableBottomMoves.returnToOrigin(picked.bottom(), picked.held());
        assertEquals(2, restored.get(5).amount());
        assertTrue(EditableBottomMoves.fullyReturned(picked.bottom(), restored, picked.held()));
    }

    @Test
    void returnToOriginAfterPartialPlaceKeepsLeftoverOrigin() {
        List<UxItem> bottom = emptyBottom();
        bottom.set(0, stack("minecraft:arrow", 10));
        EditableBottomMoves.Outcome picked = EditableBottomMoves.applyPickup(bottom, null, 0, 0);
        EditableBottomMoves.Outcome partial = EditableBottomMoves.applyPickup(
                picked.bottom(), picked.held(), 1, 1
        );
        assertNotNull(partial.held());
        assertEquals(0, partial.held().originIndex());
        assertEquals(1, partial.bottom().get(1).amount());
        assertEquals(9, partial.held().item().amount());

        List<UxItem> restored = EditableBottomMoves.returnToOrigin(partial.bottom(), partial.held());
        assertEquals(9, restored.get(0).amount());
        assertEquals(1, restored.get(1).amount());
    }

    @Test
    void debounceRejectKeepsHeldCursorState() {
        List<UxItem> bottom = emptyBottom();
        bottom.set(8, stack("minecraft:stick", 1));
        EditableBottomMoves.Outcome picked = EditableBottomMoves.applyPickup(bottom, null, 8, 0);
        assertNotNull(picked.held());
        EditableBottomMoves.Held stillHeld = picked.held();
        assertEquals("minecraft:stick", stillHeld.item().materialKey());
        assertTrue(picked.bottom().get(8).isEmpty());
    }

    private static List<UxItem> emptyBottom() {
        List<UxItem> bottom = new ArrayList<>(36);
        for (int i = 0; i < 36; i++) {
            bottom.add(UxItem.EMPTY);
        }
        return bottom;
    }

    private static UxItem stack(String key, int amount) {
        return UxItem.builder(key).amount(amount).build();
    }

    private static void assertConserved(List<UxItem> before, UxItem beforeCursor, List<UxItem> after, UxItem afterCursor) {
        assertEquals(count(before) + count(beforeCursor), count(after) + count(afterCursor));
    }

    private static int count(List<UxItem> items) {
        int total = 0;
        for (UxItem item : items) {
            total += count(item);
        }
        return total;
    }

    private static int count(UxItem item) {
        return item == null || item.isEmpty() ? 0 : item.amount();
    }
}
