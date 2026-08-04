package net.opmasterleo.packetuxui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

class SlotsTest {

    @Test
    void indexRowCol() {
        assertEquals(0, Slots.index(0, 0));
        assertEquals(13, Slots.index(1, 4));
        assertEquals(1, Slots.row(13));
        assertEquals(4, Slots.column(13));
    }

    @Test
    void borderThreeRows() {
        List<Integer> border = Slots.border(3);
        assertTrue(border.contains(0));
        assertTrue(border.contains(8));
        assertTrue(border.contains(9));
        assertTrue(border.contains(17));
        assertTrue(border.contains(18));
        assertTrue(border.contains(26));
        assertFalse(border.contains(13));
        assertEquals(new HashSet<>(border).size(), border.size());
    }

    @Test
    void rectangleAndBottom() {
        assertEquals(List.of(10, 11, 12, 19, 20, 21),
                Slots.rectangle(1, 1, 2, 3));
        assertTrue(Slots.isTop(5, 27));
        assertTrue(Slots.isBottom(30, 27));
        assertEquals(3, Slots.toBottomIndex(30, 27));
        assertEquals(30, Slots.fromBottomIndex(3, 27));
    }
}
