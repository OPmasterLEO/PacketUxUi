package net.opmasterleo.packetuxui.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.DyeColor;
import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.opmasterleo.packetuxui.service.SignResult;
import net.opmasterleo.packetuxui.service.SignView;

class SignBuildTest {

    @Test
    void padsFourEmptyLines() {
        SignView view = SignBuild.create().build();
        assertEquals(4, view.lines().length);
        assertEquals("", view.legacyLines()[0]);
        assertEquals("BLACK", view.dyeColor());
        assertFalse(view.glow());
    }

    @Test
    void linesAndIndexedLine() {
        SignView view = SignBuild.create()
                .lines("<gold>Name", "", "hint")
                .line(3, Component.text("ok"))
                .color(DyeColor.YELLOW)
                .glow(true)
                .build();
        assertEquals("Name", plain(view.lines()[0]));
        assertEquals("", plain(view.lines()[1]));
        assertEquals("hint", plain(view.lines()[2]));
        assertEquals("ok", plain(view.lines()[3]));
        assertEquals("YELLOW", view.dyeColor());
        assertTrue(view.glow());
    }

    @Test
    void parsesSectionLegacy() {
        SignView view = SignBuild.create().line(0, "§6Hello").build();
        assertEquals("Hello", plain(view.lines()[0]));
        String legacy = view.legacyLines()[0];
        assertTrue(legacy.contains("Hello"));
        assertEquals("Hello", PlainTextComponentSerializer.plainText().serialize(
                LegacyComponentSerializer.legacySection().deserialize(legacy)
        ).replace("Hello", "Hello"));
    }

    @Test
    void rejectsBadLineIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> SignBuild.create().line(4, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> SignBuild.create().line(-1, "x"));
    }

    @Test
    void resultPlainStripsColors() {
        SignResult result = new SignResult(new String[] {"§6Hi", "&cBye", "plain", null});
        assertEquals("Hi", result.plain(0));
        assertEquals("Bye", result.plain(1));
        assertEquals("plain", result.plain(2));
        assertEquals("", result.plain(3));
        assertEquals("§6Hi", result.line(0));
    }

    @Test
    void withLinesKeepsMeta() {
        SignView view = SignBuild.create().color(DyeColor.RED).glow(true).type("OAK_SIGN").build();
        SignView next = view.withLines(new Component[] {Component.text("x")});
        assertEquals("RED", next.dyeColor());
        assertTrue(next.glow());
        assertEquals("OAK_SIGN", next.materialName());
        assertEquals("x", plain(next.lines()[0]));
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
