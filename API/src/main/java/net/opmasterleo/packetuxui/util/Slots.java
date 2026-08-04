package net.opmasterleo.packetuxui.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Slot index helpers for 9-wide chest menus. Plugins compose layouts; the library does not
 * ship border/pagination products.
 */
public final class Slots {

    private Slots() {
    }

    public static int index(int row, int column) {
        if (row < 0 || column < 0 || column > 8) {
            throw new IllegalArgumentException("row>=0 and column 0..8 required");
        }
        return row * 9 + column;
    }

    public static int row(int slot) {
        return slot / 9;
    }

    public static int column(int slot) {
        return slot % 9;
    }

    /** Top/bottom rows and left/right columns for a chest with {@code rows} (1..6). */
    public static List<Integer> border(int rows) {
        int safe = Math.max(1, Math.min(6, rows));
        int size = safe * 9;
        List<Integer> out = new ArrayList<>(size);
        for (int c = 0; c < 9; c++) {
            out.add(c);
            if (safe > 1) {
                out.add((safe - 1) * 9 + c);
            }
        }
        for (int r = 1; r < safe - 1; r++) {
            out.add(r * 9);
            out.add(r * 9 + 8);
        }
        return out;
    }

    /** Inclusive rectangle in row/column space. */
    public static List<Integer> rectangle(int row0, int col0, int row1, int col1) {
        if (row1 < row0 || col1 < col0) {
            throw new IllegalArgumentException("rectangle bounds inverted");
        }
        List<Integer> out = new ArrayList<>((row1 - row0 + 1) * (col1 - col0 + 1));
        for (int r = row0; r <= row1; r++) {
            for (int c = col0; c <= col1; c++) {
                out.add(index(r, c));
            }
        }
        return out;
    }

    public static boolean isTop(int slot, int topSlotCount) {
        return slot >= 0 && slot < topSlotCount;
    }

    public static boolean isBottom(int slot, int topSlotCount) {
        return isBottom(slot, topSlotCount, 36);
    }

    public static boolean isBottom(int slot, int topSlotCount, int bottomSlotCount) {
        return slot >= topSlotCount && slot < topSlotCount + bottomSlotCount;
    }

    /** Protocol bottom slot → 0..35 inventory snapshot index (storage then hotbar). */
    public static int toBottomIndex(int protocolSlot, int topSlotCount) {
        return protocolSlot - topSlotCount;
    }

    /** 0..35 bottom snapshot index → protocol slot. */
    public static int fromBottomIndex(int bottomIndex, int topSlotCount) {
        return topSlotCount + bottomIndex;
    }
}
