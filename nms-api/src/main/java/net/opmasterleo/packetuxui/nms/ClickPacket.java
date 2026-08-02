package net.opmasterleo.packetuxui.nms;

import java.util.Collections;
import java.util.Map;

import net.opmasterleo.packetuxui.nms.item.UxItem;

public final class ClickPacket {

    private final int windowId;
    private final int stateId;
    private final int slot;
    private final int button;
    private final int actionNumber;
    private final WindowClickType clickType;
    private final Map<Integer, UxItem> changedSlots;
    private final UxItem carried;

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
        this.clickType = clickType;
        this.changedSlots = changedSlots == null ? Collections.emptyMap() : Map.copyOf(changedSlots);
        this.carried = carried;
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

    public Map<Integer, UxItem> changedSlots() {
        return changedSlots;
    }

    public UxItem carried() {
        return carried;
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
                carried
        );
    }
}
