package net.opmasterleo.packetuxui.service;

import java.util.Arrays;
import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class SignResult {

    private final String[] lines;

    public SignResult(String[] lines) {
        this.lines = pad(lines);
    }

    public String line(int index) {
        checkIndex(index);
        return lines[index];
    }

    public String plain(int index) {
        return strip(line(index));
    }

    public Component component(int index) {
        return LegacyComponentSerializer.legacySection().deserialize(line(index));
    }

    public String[] lines() {
        return lines.clone();
    }

    public String[] plainLines() {
        String[] out = new String[4];
        for (int i = 0; i < 4; i++) {
            out[i] = strip(lines[i]);
        }
        return out;
    }

    public List<Component> components() {
        return List.of(component(0), component(1), component(2), component(3));
    }

    private static String strip(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '§' || c == '&') && i + 1 < text.length()) {
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static void checkIndex(int index) {
        if (index < 0 || index > 3) {
            throw new IndexOutOfBoundsException("sign line index must be 0..3");
        }
    }

    private static String[] pad(String[] raw) {
        String[] out = new String[4];
        for (int i = 0; i < 4; i++) {
            String line = raw != null && i < raw.length ? raw[i] : null;
            out[i] = line == null ? "" : line;
        }
        return out;
    }

    @Override
    public String toString() {
        return "SignResult" + Arrays.toString(lines);
    }
}
