package net.opmasterleo.packetuxui.nms;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import net.opmasterleo.packetuxui.nms.item.UxItem;

public final class ClickPacket {

    private final int windowId;
    private final int stateId;
    private final int slot;
    private final int button;
    private final int actionNumber;
    private final WindowClickType clickType;
    private final Set<Integer> changedSlotIds;
    private final boolean carriedEmpty;
    private final Map<Integer, UxItem> changedEager;
    private final UxItem carriedEager;
    private final Supplier<Map<Integer, UxItem>> changedLazy;
    private final Supplier<UxItem> carriedLazy;

    private Map<Integer, UxItem> changedResolved;
    private UxItem carriedResolved;

    public ClickPacket(
            int windowId,
            int stateId,
            int slot,
            int button,
            int actionNumber,
            WindowClickType clickType,
            Map<Integer, UxItem> changedSlots,
            UxItem carried
    ) {
        this.windowId = windowId;
        this.stateId = stateId;
        this.slot = slot;
        this.button = button;
        this.actionNumber = actionNumber;
        this.clickType = clickType == null ? WindowClickType.UNKNOWN : clickType;
        Map<Integer, UxItem> map = normalizeMap(changedSlots);
        this.changedEager = map;
        this.changedSlotIds = map.isEmpty() ? Set.of() : Set.copyOf(map.keySet());
        this.carriedEager = carried == null || carried.isEmpty() ? UxItem.EMPTY : carried;
        this.carriedEmpty = this.carriedEager.isEmpty();
        this.changedLazy = null;
        this.carriedLazy = null;
        this.changedResolved = map;
        this.carriedResolved = this.carriedEager;
    }

    private ClickPacket(
            int windowId,
            int stateId,
            int slot,
            int button,
            int actionNumber,
            WindowClickType clickType,
            Set<Integer> changedSlotIds,
            boolean carriedEmpty,
            Supplier<Map<Integer, UxItem>> changedLazy,
            Supplier<UxItem> carriedLazy
    ) {
        this.windowId = windowId;
        this.stateId = stateId;
        this.slot = slot;
        this.button = button;
        this.actionNumber = actionNumber;
        this.clickType = clickType == null ? WindowClickType.UNKNOWN : clickType;
        this.changedSlotIds = changedSlotIds == null || changedSlotIds.isEmpty()
                ? Set.of()
                : Set.copyOf(changedSlotIds);
        this.carriedEmpty = carriedEmpty;
        this.changedEager = null;
        this.carriedEager = null;
        this.changedLazy = Objects.requireNonNull(changedLazy, "changedLazy");
        this.carriedLazy = Objects.requireNonNull(carriedLazy, "carriedLazy");
    }

    public static ClickPacket lazy(
            int windowId,
            int stateId,
            int slot,
            int button,
            int actionNumber,
            WindowClickType clickType,
            Set<Integer> changedSlotIds,
            boolean carriedEmpty,
            Supplier<Map<Integer, UxItem>> changedLazy,
            Supplier<UxItem> carriedLazy
    ) {
        return new ClickPacket(
                windowId,
                stateId,
                slot,
                button,
                actionNumber,
                clickType,
                changedSlotIds,
                carriedEmpty,
                changedLazy,
                carriedLazy
        );
    }

    public int windowId() {
        return windowId;
    }

    public int stateId() {
        return stateId;
    }

    public int slot() {
        return slot;
    }

    public int button() {
        return button;
    }

    public int actionNumber() {
        return actionNumber;
    }

    public WindowClickType clickType() {
        return clickType;
    }

    public Set<Integer> changedSlotIds() {
        return changedSlotIds;
    }

    public boolean carriedEmpty() {
        return carriedEmpty;
    }

    public Map<Integer, UxItem> changedSlots() {
        Map<Integer, UxItem> resolved = changedResolved;
        if (resolved != null) {
            return resolved;
        }
        if (changedEager != null) {
            return changedEager;
        }
        Map<Integer, UxItem> decoded = changedLazy.get();
        resolved = normalizeMap(decoded);
        changedResolved = resolved;
        return resolved;
    }

    public UxItem carried() {
        UxItem resolved = carriedResolved;
        if (resolved != null) {
            return resolved;
        }
        if (carriedEmpty) {
            carriedResolved = UxItem.EMPTY;
            return UxItem.EMPTY;
        }
        if (carriedEager != null) {
            return carriedEager;
        }
        UxItem decoded = carriedLazy.get();
        resolved = decoded == null || decoded.isEmpty() ? UxItem.EMPTY : decoded;
        carriedResolved = resolved;
        return resolved;
    }

    public ClickPacket withWindowAndSlot(int newWindowId, int newSlot, Map<Integer, UxItem> newChanged) {
        return new ClickPacket(
                newWindowId,
                stateId,
                newSlot,
                button,
                actionNumber,
                clickType,
                newChanged,
                carried()
        );
    }

    private static Map<Integer, UxItem> normalizeMap(Map<Integer, UxItem> changedSlots) {
        if (changedSlots == null || changedSlots.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(changedSlots);
    }
}
