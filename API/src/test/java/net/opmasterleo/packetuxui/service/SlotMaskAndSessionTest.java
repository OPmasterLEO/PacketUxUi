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
    void uxItemEqualsSkipsIdenticalDifferential() {
        UxItem a = UxItem.builder("minecraft:diamond").amount(3).build();
        UxItem b = UxItem.builder("minecraft:diamond").amount(3).build();
        UxItem c = UxItem.builder("minecraft:diamond").amount(4).build();
        assertEquals(a, b);
        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
    }
}
