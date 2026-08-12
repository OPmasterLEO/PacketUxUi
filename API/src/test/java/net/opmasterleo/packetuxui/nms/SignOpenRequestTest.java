package net.opmasterleo.packetuxui.nms;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;

class SignOpenRequestTest {

    @Test
    void padsLinesAndDefaults() {
        SignOpenRequest request = new SignOpenRequest(
                1, 2, 3,
                new Component[] {Component.text("a")},
                new String[] {"a"},
                null,
                true,
                "OAK_SIGN"
        );
        assertEquals(1, request.x());
        assertEquals(2, request.y());
        assertEquals(3, request.z());
        assertEquals(4, request.lines().length);
        assertEquals(4, request.legacyLines().length);
        assertEquals("a", request.legacyLines()[0]);
        assertEquals("", request.legacyLines()[1]);
        assertEquals("BLACK", request.dyeColor());
        assertTrue(request.glow());
        assertEquals("OAK_SIGN", request.materialName());
    }

    @Test
    void updateMatchesPosition() {
        SignUpdate update = new SignUpdate(4, 5, 6, new String[] {"x"}, true);
        assertTrue(update.matches(4, 5, 6));
        assertFalse(update.matches(4, 5, 7));
        assertArrayEquals(new String[] {"x", "", "", ""}, update.lines());
    }
}
