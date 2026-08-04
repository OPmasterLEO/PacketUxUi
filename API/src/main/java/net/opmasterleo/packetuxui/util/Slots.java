package net.opmasterleo.packetuxui.util;

import java.util.ArrayList;
import java.util.List;

import net.opmasterleo.packetuxui.nms.LiveLimits;

/**
 * Slot index helpers for chest menus. Column width comes from live NMS hotbar size.
 */
public final class Slots {

    private Slots() {
    }

    public static int columns() {
        return LiveLimits.hotbarSlots();
    }

    public static int index(int row, int column) {
        int cols = columns();
        if (row < 0 || column < 0 || column >= cols) {
            throw new IllegalArgumentException("row>=0 and column 0.." + (cols - 1) + " required");
        }
        return row * cols + column;
    }

    public static int row(int slot) {
        return slot / columns();
    }

    public static int column(int slot) {
        return slot % columns();
    }

    /** Top/bottom rows and left/right columns for a chest with {@code rows}. */
    public static List<Integer> border(int rows) {
        int cols = columns();
        int maxRows = LiveLimits.maxGenericChestRows();
        int safe = Math.max(1, Math.min(maxRows, rows));
        int size = safe * cols;
        List<Integer> out = new ArrayList<>(size);
        for (int c = 0; c < cols; c++) {
            out.add(c);
            if (safe > 1) {
                out.add((safe - 1) * cols + c);
            }
        }
        for (int r = 1; r < safe - 1; r++) {
            out.add(r * cols);
            out.add(r * cols + (cols - 1));
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
        return isBottom(slot, topSlotCount, LiveLimits.playerInventorySlots());
    }

    public static boolean isBottom(int slot, int topSlotCount, int bottomSlotCount) {
        return slot >= topSlotCount && slot < topSlotCount + bottomSlotCount;
    }

    /** Protocol bottom slot → inventory snapshot index (storage then hotbar). */
    public static int toBottomIndex(int protocolSlot, int topSlotCount) {
        return protocolSlot - topSlotCount;
    }

    /** Bottom snapshot index → protocol slot. */
    public static int fromBottomIndex(int bottomIndex, int topSlotCount) {
        return topSlotCount + bottomIndex;
    }
}
