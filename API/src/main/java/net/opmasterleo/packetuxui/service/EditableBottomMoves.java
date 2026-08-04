package net.opmasterleo.packetuxui.service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import net.opmasterleo.packetuxui.nms.WindowClickType;
import net.opmasterleo.packetuxui.nms.item.UxItem;

public final class EditableBottomMoves {

    private static final Predicate<Integer> ANY = slot -> true;

    public record Held(UxItem item, int originIndex) {
        public Held {
            item = item == null || item.isEmpty() ? UxItem.EMPTY : item;
        }

        public boolean isEmpty() {
            return item.isEmpty();
        }
    }

    public record Outcome(List<UxItem> bottom, Held held, Set<Integer> dirty) {
        public Outcome {
            bottom = List.copyOf(bottom);
            dirty = dirty == null || dirty.isEmpty() ? Set.of() : Set.copyOf(dirty);
        }
    }

    private EditableBottomMoves() {
    }

    public static Outcome applyPickup(List<UxItem> bottom, Held previous, int slotIndex, int button) {
        Objects.requireNonNull(bottom, "bottom");
        UxItem heldItem = previous == null || previous.isEmpty() ? UxItem.EMPTY : previous.item();
        VirtualClickSimulator.Result result = VirtualClickSimulator.simulate(
                bottom,
                heldItem,
                slotIndex,
                button,
                WindowClickType.PICKUP,
                ANY
        );
        Held next = nextHeld(previous, heldItem, result.cursor(), slotIndex);
        return new Outcome(result.items(), next, result.dirty());
    }

    public static Held nextHeld(Held previous, UxItem beforeCursor, UxItem afterCursor, int clickedIndex) {
        if (afterCursor == null || afterCursor.isEmpty()) {
            return null;
        }
        if (beforeCursor == null || beforeCursor.isEmpty()) {
            return new Held(afterCursor, clickedIndex);
        }
        if (previous != null && afterCursor.isSimilar(beforeCursor) && afterCursor.amount() < beforeCursor.amount()) {
            return new Held(afterCursor, previous.originIndex());
        }
        if (previous != null && afterCursor.equals(beforeCursor)) {
            return new Held(afterCursor, previous.originIndex());
        }
        return new Held(afterCursor, clickedIndex);
    }

    public static List<UxItem> returnToOrigin(List<UxItem> bottom, Held held) {
        if (held == null || held.isEmpty()) {
            return List.copyOf(bottom);
        }
        List<UxItem> next = new java.util.ArrayList<>(bottom);
        int idx = held.originIndex();
        if (idx < 0 || idx >= next.size()) {
            return next;
        }
        UxItem at = next.get(idx);
        UxItem item = held.item();
        if (at.isEmpty()) {
            next.set(idx, item);
            return next;
        }
        if (at.isSimilar(item)) {
            int max = VirtualClickSimulator.maxStack(item);
            int room = max - at.amount();
            if (room >= item.amount()) {
                next.set(idx, at.withAmount(at.amount() + item.amount()));
            }
        }
        return next;
    }

    public static boolean fullyReturned(List<UxItem> beforeReturn, List<UxItem> afterReturn, Held held) {
        if (held == null || held.isEmpty()) {
            return true;
        }
        int idx = held.originIndex();
        if (idx < 0 || idx >= afterReturn.size()) {
            return false;
        }
        UxItem at = afterReturn.get(idx);
        if (beforeReturn.get(idx).isEmpty()) {
            return at.isSimilar(held.item()) && at.amount() >= held.item().amount();
        }
        return at.isSimilar(held.item()) && at.amount() >= beforeReturn.get(idx).amount() + held.item().amount();
    }
}
