package net.opmasterleo.packetuxui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

class SignActionTest {

    @Test
    void closeAndReopenKinds() {
        assertEquals(SignAction.Kind.CLOSE, SignAction.close().kind());
        assertEquals(SignAction.Kind.REOPEN, SignAction.reopen().kind());
        assertNull(SignAction.reopen().lines());
    }

    @Test
    void reopenPadsLines() {
        SignAction action = SignAction.reopen("a", "b");
        assertEquals("a", plain(action.lines()[0]));
        assertEquals("b", plain(action.lines()[1]));
        assertEquals("", plain(action.lines()[2]));
        assertEquals("", plain(action.lines()[3]));
    }

    @Test
    void closeThenKeepsRunnable() {
        Runnable run = () -> {
        };
        assertSame(run, SignAction.closeThen(run).after());
        assertEquals(SignAction.Kind.CLOSE, SignAction.closeThen(run).kind());
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
