package net.opmasterleo.packetuxui.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.Material;

import net.opmasterleo.packetuxui.nms.WindowClickType;
import net.opmasterleo.packetuxui.nms.item.UxItem;

public final class VirtualClickSimulator {

    public record Result(List<UxItem> items, UxItem cursor, Set<Integer> dirty) {
        public Result {
            items = List.copyOf(items);
            cursor = cursor == null || cursor.isEmpty() ? UxItem.EMPTY : cursor;
            dirty = Set.copyOf(dirty);
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
        List<UxItem> items = new ArrayList<>(top);
        UxItem held = cursor == null || cursor.isEmpty() ? UxItem.EMPTY : cursor;
        Set<Integer> dirty = new HashSet<>();
        if (slot < 0 || slot >= items.size() || !takeable.test(slot)) {
            return new Result(items, held, dirty);
        }
        return switch (type) {
            case PICKUP -> pickup(items, held, slot, button, dirty);
            case QUICK_MOVE -> shift(items, held, slot, takeable, dirty);
            case PICKUP_ALL -> pickupAll(items, held, slot, takeable, dirty);
            default -> new Result(items, held, dirty);
        };
    }

    public static Result dragEnd(
            List<UxItem> top,
            UxItem cursor,
            List<Integer> slots,
            int button,
            Predicate<Integer> takeable
    ) {
        List<UxItem> items = new ArrayList<>(top);
        UxItem held = cursor == null || cursor.isEmpty() ? UxItem.EMPTY : cursor;
        Set<Integer> dirty = new HashSet<>();
        if (held.isEmpty() || slots.isEmpty()) {
            return new Result(items, held, dirty);
        }
        List<Integer> targets = new ArrayList<>();
        for (Integer s : slots) {
            if (s != null && s >= 0 && s < items.size() && takeable.test(s)) {
                UxItem at = items.get(s);
                if (at.isEmpty() || at.isSimilar(held)) {
                    targets.add(s);
                }
            }
        }
        if (button == 10) {
            return new Result(items, held, dirty);
        }
        if (targets.isEmpty()) {
            return new Result(items, held, dirty);
        }
        boolean oneEach = button == 6;
        int remaining = held.amount();
        if (oneEach) {
            for (int s : targets) {
                if (remaining <= 0) {
                    break;
                }
                UxItem at = items.get(s);
                int max = maxStack(held);
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
                int max = maxStack(held);
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
                int max = maxStack(held);
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
        String name = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT)).getMaxStackSize();
        } catch (IllegalArgumentException ignored) {
            return 64;
        }
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

    public static Result shiftInto(List<UxItem> top, UxItem moving, Predicate<Integer> placeable) {
        List<UxItem> items = new ArrayList<>(top);
        Set<Integer> dirty = new HashSet<>();
        if (moving == null || moving.isEmpty()) {
            return new Result(items, UxItem.EMPTY, dirty);
        }
        int remaining = moving.amount();
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            if (!placeable.test(i)) {
                continue;
            }
            UxItem at = items.get(i);
            if (!at.isSimilar(moving)) {
                continue;
            }
            int max = maxStack(moving);
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
            int max = maxStack(moving);
            int place = Math.min(max, remaining);
            items.set(i, moving.withAmount(place));
            dirty.add(i);
            remaining -= place;
        }
        UxItem left = remaining <= 0 ? UxItem.EMPTY : moving.withAmount(remaining);
        return new Result(items, left, dirty);
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
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            if (i == slot || !takeable.test(i)) {
                continue;
            }
            UxItem at = items.get(i);
            if (!at.isSimilar(moving)) {
                continue;
            }
            int max = maxStack(moving);
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
            if (i == slot || !takeable.test(i)) {
                continue;
            }
            if (!items.get(i).isEmpty()) {
                continue;
            }
            int max = maxStack(moving);
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
        for (int i = 0; i < items.size() && total < max; i++) {
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
