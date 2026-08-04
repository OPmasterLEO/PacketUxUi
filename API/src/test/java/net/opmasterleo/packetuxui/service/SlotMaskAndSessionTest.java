package net.opmasterleo.packetuxui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.types.InventoryType;

class SlotMaskAndSessionTest {

    @Test
    void slotMaskContainsIsO1() {
        SlotMask mask = SlotMask.of(0, 2, 5);
        assertTrue(mask.contains(0));
        assertFalse(mask.contains(1));
        assertTrue(mask.contains(5));
        mask.clear(5);
        assertFalse(mask.contains(5));
        SlotMask range = SlotMask.range(3, 6);
        assertTrue(range.contains(3));
        assertTrue(range.contains(5));
        assertFalse(range.contains(6));
    }

    @Test
    void sessionSlotKinds() {
        Button action = Button.builder()
                .item(UxItem.builder("minecraft:stone").build())
                .action()
                .build();
        Button editable = Button.builder()
                .item(UxItem.EMPTY)
                .editable()
                .build();
        Menu menu = new Menu(
                Component.text("t"),
                InventoryType.GENERIC9X1,
                java.util.Map.of(0, action, 1, editable),
                new net.opmasterleo.packetuxui.dto.CooldownComponent(),
                MenuMode.EDITABLE
        );
        MenuSession session = new MenuSession(menu, 100);
        assertEquals(SlotKind.ACTION, session.slotKind(0));
        assertEquals(SlotKind.EDITABLE, session.slotKind(1));
        assertEquals(SlotKind.EDITABLE, session.slotKind(2));
        assertEquals(SessionPhase.OPEN, session.phase());
        int g1 = session.generation();
        assertEquals(g1 + 1, session.bumpGeneration());
        session.setPhase(SessionPhase.CLOSING);
        assertEquals(SessionPhase.CLOSING, session.phase());
    }

    @Test
    void nextStateIdAboveBeatsClientPrediction() {
        Menu menu = new Menu(
                Component.text("t"),
                InventoryType.GENERIC9X1,
                java.util.Map.of(),
                new net.opmasterleo.packetuxui.dto.CooldownComponent(),
                MenuMode.READ_ONLY
        );
        MenuSession session = new MenuSession(menu, 100);
        assertEquals(1, session.nextStateId());
        assertEquals(2, session.nextStateId());
        // Client predicted ahead of us — correction must be strictly greater.
        assertEquals(10, session.nextStateIdAbove(9));
        assertEquals(11, session.nextStateIdAbove(5));
        assertEquals(12, session.nextStateIdAbove(-1));
    }
}
