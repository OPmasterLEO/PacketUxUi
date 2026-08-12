package net.opmasterleo.packetuxui.service;

import net.kyori.adventure.text.Component;

public final class SignAction {

    enum Kind {
        CLOSE,
        REOPEN
    }

    private final Kind kind;
    private final Component[] lines;
    private final Runnable after;

    private SignAction(Kind kind, Component[] lines, Runnable after) {
        this.kind = kind;
        this.lines = lines;
        this.after = after;
    }

    public static SignAction close() {
        return new SignAction(Kind.CLOSE, null, null);
    }

    public static SignAction closeThen(Runnable after) {
        return new SignAction(Kind.CLOSE, null, after);
    }

    public static SignAction reopen() {
        return new SignAction(Kind.REOPEN, null, null);
    }

    public static SignAction reopen(String... miniMessageLines) {
        Component[] lines = new Component[4];
        for (int i = 0; i < 4; i++) {
            String raw = miniMessageLines != null && i < miniMessageLines.length ? miniMessageLines[i] : null;
            lines[i] = raw == null ? Component.empty() : SignView.parseLine(raw);
        }
        return new SignAction(Kind.REOPEN, lines, null);
    }

    public static SignAction reopen(Component... lines) {
        Component[] copy = new Component[4];
        for (int i = 0; i < 4; i++) {
            Component line = lines != null && i < lines.length ? lines[i] : null;
            copy[i] = line == null ? Component.empty() : line;
        }
        return new SignAction(Kind.REOPEN, copy, null);
    }

    Kind kind() {
        return kind;
    }

    Component[] lines() {
        return lines;
    }

    Runnable after() {
        return after;
    }
}
