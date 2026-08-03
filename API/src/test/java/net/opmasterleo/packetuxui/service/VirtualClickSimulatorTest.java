package net.opmasterleo.packetuxui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import net.opmasterleo.packetuxui.nms.WindowClickType;
import net.opmasterleo.packetuxui.nms.item.UxItem;

class VirtualClickSimulatorTest {

    private static final Predicate<Integer> ALL = slot -> true;

    @Test
    void leftPickupAndPlaceConserves() {
        List<UxItem> top = list(stack("minecraft:stone", 32), UxItem.EMPTY);
        VirtualClickSimulator.Result picked = VirtualClickSimulator.simulate(
                top, UxItem.EMPTY, 0, 0, WindowClickType.PICKUP, ALL
        );
        assertConserved(top, UxItem.EMPTY, picked.items(), picked.cursor());
        VirtualClickSimulator.Result placed = VirtualClickSimulator.simulate(
                picked.items(), picked.cursor(), 1, 0, WindowClickType.PICKUP, ALL
        );
        assertConserved(picked.items(), picked.cursor(), placed.items(), placed.cursor());
        assertTrue(placed.cursor().isEmpty());
        assertEquals(32, placed.items().get(1).amount());
    }

    @Test
    void rightSplitConserves() {
        List<UxItem> top = list(stack("minecraft:dirt", 5));
        VirtualClickSimulator.Result r = VirtualClickSimulator.simulate(
                top, UxItem.EMPTY, 0, 1, WindowClickType.PICKUP, ALL
        );
        assertConserved(top, UxItem.EMPTY, r.items(), r.cursor());
        assertEquals(2, r.items().get(0).amount());
        assertEquals(3, r.cursor().amount());
    }

    @Test
    void swapDifferentConserves() {
        List<UxItem> top = list(stack("minecraft:stone", 10));
        UxItem cursor = stack("minecraft:dirt", 4);
        VirtualClickSimulator.Result r = VirtualClickSimulator.simulate(
                top, cursor, 0, 0, WindowClickType.PICKUP, ALL
        );
        assertConserved(top, cursor, r.items(), r.cursor());
        assertEquals("minecraft:dirt", r.items().get(0).materialKey());
        assertEquals("minecraft:stone", r.cursor().materialKey());
    }

    @Test
    void mergeConserves() {
        List<UxItem> top = list(stack("minecraft:stone", 60));
        UxItem cursor = stack("minecraft:stone", 10);
        VirtualClickSimulator.Result r = VirtualClickSimulator.simulate(
                top, cursor, 0, 0, WindowClickType.PICKUP, ALL
        );
        assertConserved(top, cursor, r.items(), r.cursor());
        assertEquals(64, r.items().get(0).amount());
        assertEquals(6, r.cursor().amount());
    }

    @Test
    void shiftClickConserves() {
        List<UxItem> top = list(
                stack("minecraft:stone", 20),
                stack("minecraft:stone", 50),
                UxItem.EMPTY
        );
        VirtualClickSimulator.Result r = VirtualClickSimulator.simulate(
                top, UxItem.EMPTY, 0, 0, WindowClickType.QUICK_MOVE, ALL
        );
        assertConserved(top, UxItem.EMPTY, r.items(), r.cursor());
        assertTrue(r.items().get(0).isEmpty());
        assertEquals(64, r.items().get(1).amount());
        assertEquals(6, r.items().get(2).amount());
    }

    @Test
    void pickupAllConserves() {
        List<UxItem> top = list(
                stack("minecraft:cobblestone", 10),
                stack("minecraft:cobblestone", 15),
                stack("minecraft:dirt", 8)
        );
        VirtualClickSimulator.Result r = VirtualClickSimulator.simulate(
                top, UxItem.EMPTY, 0, 0, WindowClickType.PICKUP_ALL, ALL
        );
        assertConserved(top, UxItem.EMPTY, r.items(), r.cursor());
        assertEquals(25, r.cursor().amount());
        assertEquals(8, r.items().get(2).amount());
    }

    @Test
    void dragEndConserves() {
        List<UxItem> top = list(UxItem.EMPTY, UxItem.EMPTY, UxItem.EMPTY);
        UxItem cursor = stack("minecraft:arrow", 9);
        VirtualClickSimulator.Result r = VirtualClickSimulator.dragEnd(
                top, cursor, List.of(0, 1, 2), 2, ALL
        );
        assertConserved(top, cursor, r.items(), r.cursor());
        assertEquals(3, r.items().get(0).amount());
        assertEquals(3, r.items().get(1).amount());
        assertEquals(3, r.items().get(2).amount());
        assertTrue(r.cursor().isEmpty());
    }

    @Test
    void shiftIntoMergesThenEmpty() {
        List<UxItem> top = list(
                stack("minecraft:stone", 60),
                UxItem.EMPTY,
                stack("minecraft:dirt", 1)
        );
        Predicate<Integer> editable = slot -> slot == 0 || slot == 1;
        VirtualClickSimulator.Result r = VirtualClickSimulator.shiftInto(
                top, stack("minecraft:stone", 10), editable
        );
        assertEquals(64, r.items().get(0).amount());
        assertEquals(6, r.items().get(1).amount());
        assertEquals(1, r.items().get(2).amount());
        assertTrue(r.cursor().isEmpty());
        assertConserved(
                List.of(stack("minecraft:stone", 60), UxItem.EMPTY, stack("minecraft:dirt", 1)),
                stack("minecraft:stone", 10),
                r.items(),
                r.cursor()
        );
    }

    @Test
    void dragSkipsNonTakeableSlotsWithoutLoss() {
        List<UxItem> top = list(UxItem.EMPTY, stack("minecraft:barrier", 1), UxItem.EMPTY);
        UxItem cursor = stack("minecraft:arrow", 3);
        Predicate<Integer> editableOnly = slot -> slot == 0 || slot == 2;
        VirtualClickSimulator.Result r = VirtualClickSimulator.dragEnd(
                top, cursor, List.of(0, 1, 2), 2, editableOnly
        );
        assertConserved(top, cursor, r.items(), r.cursor());
        assertEquals(2, r.items().get(0).amount());
        assertEquals(1, r.items().get(1).amount()); // untouched locked slot
        assertEquals(1, r.items().get(2).amount());
        assertTrue(r.cursor().isEmpty());
    }

    @Test
    void lockedSlotShiftIsNoOp() {
        List<UxItem> top = list(stack("minecraft:stone", 8), UxItem.EMPTY);
        Predicate<Integer> none = slot -> false;
        VirtualClickSimulator.Result r = VirtualClickSimulator.simulate(
                top, UxItem.EMPTY, 0, 0, WindowClickType.QUICK_MOVE, none
        );
        assertEquals(top, r.items());
        assertTrue(r.cursor().isEmpty());
    }

    private static void assertConserved(List<UxItem> beforeTop, UxItem beforeCursor, List<UxItem> afterTop, UxItem afterCursor) {
        assertEquals(totals(beforeTop, beforeCursor), totals(afterTop, afterCursor));
    }

    private static Map<String, Integer> totals(List<UxItem> top, UxItem cursor) {
        Map<String, Integer> map = new HashMap<>();
        for (UxItem item : top) {
            add(map, item);
        }
        add(map, cursor);
        return map;
    }

    private static void add(Map<String, Integer> map, UxItem item) {
        if (item == null || item.isEmpty()) {
            return;
        }
        String key = item.materialKey() + "|" + item.name() + "|" + item.enchantments();
        map.merge(key, item.amount(), Integer::sum);
    }

    private static List<UxItem> list(UxItem... items) {
        List<UxItem> list = new ArrayList<>();
        for (UxItem item : items) {
            list.add(item);
        }
        return list;
    }

    private static UxItem stack(String key, int amount) {
        return UxItem.builder(key).amount(amount).build();
    }
}
