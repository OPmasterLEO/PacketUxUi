package net.opmasterleo.packetuxui.service;

import java.util.BitSet;

public final class SlotMask {

    private final BitSet bits;

    public SlotMask() {
        this.bits = new BitSet();
    }

    public SlotMask(int sizeHint) {
        this.bits = new BitSet(Math.max(0, sizeHint));
    }

    public static SlotMask of(int... slots) {
        SlotMask mask = new SlotMask(slots.length == 0 ? 0 : slots[slots.length - 1] + 1);
        for (int slot : slots) {
            if (slot >= 0) {
                mask.bits.set(slot);
            }
        }
        return mask;
    }

    public static SlotMask range(int fromInclusive, int toExclusive) {
        SlotMask mask = new SlotMask(toExclusive);
        if (fromInclusive < toExclusive) {
            mask.bits.set(fromInclusive, toExclusive);
        }
        return mask;
    }

    public SlotMask set(int slot) {
        if (slot >= 0) {
            bits.set(slot);
        }
        return this;
    }

    public SlotMask clear(int slot) {
        if (slot >= 0) {
            bits.clear(slot);
        }
        return this;
    }

    public boolean contains(int slot) {
        return slot >= 0 && bits.get(slot);
    }

    public BitSet bitSet() {
        return (BitSet) bits.clone();
    }
}
