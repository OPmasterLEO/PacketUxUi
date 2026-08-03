package net.opmasterleo.packetuxui.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.bukkit.Material;

import net.opmasterleo.packetuxui.nms.WindowClickType;
import net.opmasterleo.packetuxui.nms.item.UxItem;

public final class VirtualClickSimulator {

    private static final Set<Integer> NO_DIRTY = Set.of();
    private static final ConcurrentHashMap<String, Integer> MAX_STACK_CACHE = new ConcurrentHashMap<>();

    public record Result(List<UxItem> items, UxItem cursor, Set<Integer> dirty) {
        public Result {
            items = List.copyOf(items);
            cursor = cursor == null || cursor.isEmpty() ? UxItem.EMPTY : cursor;
            dirty = dirty == null || dirty.isEmpty() ? NO_DIRTY : Set.copyOf(dirty);
        }
    }

    private VirtualClickSimulator() {
    }

    public static Result simulate(
            List<UxItem> top,
            UxItem cursor,
            int slot,
            int button,
            WindowClickType type,
            Predicate<Integer> takeable
    ) {
        UxItem held = cursor == null || cursor.isEmpty() ? UxItem.EMPTY : cursor;
        if (slot < 0 || slot >= top.size() || !takeable.test(slot)) {
            return unchanged(top, held);
        }
        return switch (type) {
            case PICKUP -> {
                List<UxItem> items = new ArrayList<>(top);
                yield pickup(items, held, slot, button, new HashSet<>(4));
            }
            case QUICK_MOVE -> {
                List<UxItem> items = new ArrayList<>(top);
                yield shift(items, held, slot, takeable, new HashSet<>(8));
            }
            case PICKUP_ALL -> {
                List<UxItem> items = new ArrayList<>(top);
                yield pickupAll(items, held, slot, takeable, new HashSet<>(8));
            }
            default -> unchanged(top, held);
        };
    }

    public static Result dragEnd(
            List<UxItem> top,
            UxItem cursor,
            List<Integer> slots,
            int button,
            Predicate<Integer> takeable
    ) {
        UxItem held = cursor == null || cursor.isEmpty() ? UxItem.EMPTY : cursor;
        if (held.isEmpty() || slots.isEmpty() || button == 10) {
            return unchanged(top, held);
        }
        List<UxItem> items = new ArrayList<>(top);
        List<Integer> targets = new ArrayList<>(slots.size());
        for (Integer s : slots) {
            if (s != null && s >= 0 && s < items.size() && takeable.test(s)) {
                UxItem at = items.get(s);
                if (at.isEmpty() || at.isSimilar(held)) {
                    targets.add(s);
                }
            }
        }
        if (targets.isEmpty()) {
            return unchanged(top, held);
        }
        Set<Integer> dirty = new HashSet<>(targets.size());
        boolean oneEach = button == 6;
        int remaining = held.amount();
        int max = maxStack(held);
        if (oneEach) {
            for (int s : targets) {
                if (remaining <= 0) {
                    break;
                }
                UxItem at = items.get(s);
                int room = at.isEmpty() ? max : max - at.amount();
                if (room <= 0) {
                    continue;
                }
                items.set(s, at.isEmpty() ? held.withAmount(1) : at.withAmount(at.amount() + 1));
                dirty.add(s);
                remaining--;
            }
        } else {
            int per = Math.max(1, remaining / targets.size());
            for (int s : targets) {
                if (remaining <= 0) {
                    break;
                }
                UxItem at = items.get(s);
                int room = at.isEmpty() ? max : max - at.amount();
                if (room <= 0) {
                    continue;
                }
                int place = Math.min(per, Math.min(room, remaining));
                items.set(s, at.isEmpty() ? held.withAmount(place) : at.withAmount(at.amount() + place));
                dirty.add(s);
                remaining -= place;
            }
            for (int s : targets) {
                if (remaining <= 0) {
                    break;
                }
                UxItem at = items.get(s);
                int room = max - at.amount();
                if (room <= 0) {
                    continue;
                }
                items.set(s, at.withAmount(at.amount() + 1));
                dirty.add(s);
                remaining--;
            }
        }
        UxItem nextCursor = remaining <= 0 ? UxItem.EMPTY : held.withAmount(remaining);
        return new Result(items, nextCursor, dirty);
    }

    public static int maxStack(UxItem item) {
        if (item == null || item.isEmpty()) {
            return 64;
        }
        String key = item.materialKey();
        Integer cached = MAX_STACK_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        String name = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        int size;
        try {
            size = Material.valueOf(name.toUpperCase(Locale.ROOT)).getMaxStackSize();
        } catch (IllegalArgumentException ignored) {
            size = 64;
        }
        MAX_STACK_CACHE.put(key, size);
        return size;
    }

    public static Result shiftInto(List<UxItem> top, UxItem moving, Predicate<Integer> placeable) {
        if (moving == null || moving.isEmpty()) {
            return unchanged(top, UxItem.EMPTY);
        }
        List<UxItem> items = new ArrayList<>(top);
        Set<Integer> dirty = new HashSet<>(8);
        int remaining = moving.amount();
        int max = maxStack(moving);
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            if (!placeable.test(i)) {
                continue;
            }
            UxItem at = items.get(i);
            if (!at.isSimilar(moving)) {
                continue;
            }
            int room = max - at.amount();
            if (room <= 0) {
                continue;
            }
            int place = Math.min(room, remaining);
            items.set(i, at.withAmount(at.amount() + place));
            dirty.add(i);
            remaining -= place;
        }
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            if (!placeable.test(i) || !items.get(i).isEmpty()) {
                continue;
            }
            int place = Math.min(max, remaining);
            items.set(i, moving.withAmount(place));
            dirty.add(i);
            remaining -= place;
        }
        if (dirty.isEmpty()) {
            return unchanged(top, moving);
        }
        UxItem left = remaining <= 0 ? UxItem.EMPTY : moving.withAmount(remaining);
        return new Result(items, left, dirty);
    }

    private static Result unchanged(List<UxItem> top, UxItem cursor) {
        return new Result(top, cursor, NO_DIRTY);
    }

    private static Result pickup(List<UxItem> items, UxItem held, int slot, int button, Set<Integer> dirty) {
        UxItem at = items.get(slot);
        boolean right = button == 1;
        if (held.isEmpty()) {
            if (at.isEmpty()) {
                return new Result(items, held, dirty);
            }
            if (right) {
                int take = (at.amount() + 1) / 2;
                int leave = at.amount() - take;
                items.set(slot, leave <= 0 ? UxItem.EMPTY : at.withAmount(leave));
                dirty.add(slot);
                return new Result(items, at.withAmount(take), dirty);
            }
            items.set(slot, UxItem.EMPTY);
            dirty.add(slot);
            return new Result(items, at, dirty);
        }
        if (at.isEmpty()) {
            if (right) {
                items.set(slot, held.withAmount(1));
                dirty.add(slot);
                int left = held.amount() - 1;
                return new Result(items, left <= 0 ? UxItem.EMPTY : held.withAmount(left), dirty);
            }
            items.set(slot, held);
            dirty.add(slot);
            return new Result(items, UxItem.EMPTY, dirty);
        }
        if (at.isSimilar(held)) {
            int max = maxStack(held);
            int room = max - at.amount();
            if (room <= 0) {
                return new Result(items, held, dirty);
            }
            int place = right ? 1 : Math.min(room, held.amount());
            items.set(slot, at.withAmount(at.amount() + place));
            dirty.add(slot);
            int left = held.amount() - place;
            return new Result(items, left <= 0 ? UxItem.EMPTY : held.withAmount(left), dirty);
        }
        items.set(slot, held);
        dirty.add(slot);
        return new Result(items, at, dirty);
    }

    private static Result shift(
            List<UxItem> items,
            UxItem held,
            int slot,
            Predicate<Integer> takeable,
            Set<Integer> dirty
    ) {
        UxItem moving = items.get(slot);
        if (moving.isEmpty()) {
            return new Result(items, held, dirty);
        }
        int remaining = moving.amount();
        int max = maxStack(moving);
        int size = items.size();
        for (int i = 0; i < size && remaining > 0; i++) {
            if (i == slot || !takeable.test(i)) {
                continue;
            }
            UxItem at = items.get(i);
            if (!at.isSimilar(moving)) {
                continue;
            }
            int room = max - at.amount();
            if (room <= 0) {
                continue;
            }
            int place = Math.min(room, remaining);
            items.set(i, at.withAmount(at.amount() + place));
            dirty.add(i);
            remaining -= place;
        }
        for (int i = 0; i < size && remaining > 0; i++) {
            if (i == slot || !takeable.test(i)) {
                continue;
            }
            if (!items.get(i).isEmpty()) {
                continue;
            }
            int place = Math.min(max, remaining);
            items.set(i, moving.withAmount(place));
            dirty.add(i);
            remaining -= place;
        }
        items.set(slot, remaining <= 0 ? UxItem.EMPTY : moving.withAmount(remaining));
        dirty.add(slot);
        return new Result(items, held, dirty);
    }

    private static Result pickupAll(
            List<UxItem> items,
            UxItem held,
            int slot,
            Predicate<Integer> takeable,
            Set<Integer> dirty
    ) {
        UxItem target = held.isEmpty() ? items.get(slot) : held;
        if (target.isEmpty()) {
            return new Result(items, held, dirty);
        }
        int max = maxStack(target);
        int total = held.isEmpty() ? 0 : held.amount();
        if (held.isEmpty()) {
            items.set(slot, UxItem.EMPTY);
            dirty.add(slot);
            total = target.amount();
        }
        int size = items.size();
        for (int i = 0; i < size && total < max; i++) {
            if (!takeable.test(i)) {
                continue;
            }
            UxItem at = items.get(i);
            if (!at.isSimilar(target)) {
                continue;
            }
            int room = max - total;
            int take = Math.min(room, at.amount());
            int leave = at.amount() - take;
            items.set(i, leave <= 0 ? UxItem.EMPTY : at.withAmount(leave));
            dirty.add(i);
            total += take;
        }
        return new Result(items, target.withAmount(total), dirty);
    }
}
