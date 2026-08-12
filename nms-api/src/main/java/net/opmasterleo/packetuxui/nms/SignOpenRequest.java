package net.opmasterleo.packetuxui.nms;

import java.util.Arrays;

import net.kyori.adventure.text.Component;

public final class SignOpenRequest {

    private final int x;
    private final int y;
    private final int z;
    private final Component[] lines;
    private final String[] legacyLines;
    private final String dyeColor;
    private final boolean glow;
    private final String materialName;

    public SignOpenRequest(
            int x,
            int y,
            int z,
            Component[] lines,
            String[] legacyLines,
            String dyeColor,
            boolean glow,
            String materialName
    ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.lines = padComponents(lines);
        this.legacyLines = padLegacy(legacyLines);
        this.dyeColor = dyeColor == null || dyeColor.isEmpty() ? "BLACK" : dyeColor;
        this.glow = glow;
        this.materialName = materialName;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public Component[] lines() {
        return lines;
    }

    public String[] legacyLines() {
        return legacyLines;
    }

    public String dyeColor() {
        return dyeColor;
    }

    public boolean glow() {
        return glow;
    }

    public String materialName() {
        return materialName;
    }

    private static Component[] padComponents(Component[] raw) {
        Component[] out = new Component[4];
        for (int i = 0; i < 4; i++) {
            Component line = raw != null && i < raw.length ? raw[i] : null;
            out[i] = line == null ? Component.empty() : line;
        }
        return out;
    }

    private static String[] padLegacy(String[] raw) {
        String[] out = new String[4];
        for (int i = 0; i < 4; i++) {
            String line = raw != null && i < raw.length ? raw[i] : null;
            out[i] = line == null ? "" : line;
        }
        return out;
    }

    @Override
    public String toString() {
        return "SignOpenRequest{pos=" + x + "," + y + "," + z
                + " color=" + dyeColor
                + " glow=" + glow
                + " type=" + materialName
                + " lines=" + Arrays.toString(legacyLines)
                + "}";
    }
}
