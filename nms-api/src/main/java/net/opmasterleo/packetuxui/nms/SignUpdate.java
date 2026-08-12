package net.opmasterleo.packetuxui.nms;

import java.util.Arrays;

public final class SignUpdate {

    private final int x;
    private final int y;
    private final int z;
    private final String[] lines;
    private final boolean front;

    public SignUpdate(int x, int y, int z, String[] lines, boolean front) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.lines = pad(lines);
        this.front = front;
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

    public String[] lines() {
        return lines;
    }

    public boolean front() {
        return front;
    }

    public boolean matches(int x, int y, int z) {
        return this.x == x && this.y == y && this.z == z;
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
        return "SignUpdate{pos=" + x + "," + y + "," + z
                + " front=" + front
                + " lines=" + Arrays.toString(lines)
                + "}";
    }
}
