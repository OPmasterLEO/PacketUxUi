package net.opmasterleo.packetuxui.service;

import java.util.Objects;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.types.InventoryType;

public final class MenuSession {

    private final Menu menu;
    private final int windowId;
    private int stateId;
    private SessionPhase phase = SessionPhase.OPEN;
    private int generation;
    private long lastClickNanos;
    private Component title;

    public MenuSession(Menu menu, int windowId) {
        this.menu = Objects.requireNonNull(menu, "menu");
        this.windowId = windowId;
        this.stateId = 0;
        this.generation = 1;
        this.title = menu.name();
    }

    public Menu menu() {
        return menu;
    }

    public int windowId() {
        return windowId;
    }

    public int stateId() {
        return stateId;
    }

    public int nextStateId() {
        stateId = stateId == Integer.MAX_VALUE ? 1 : stateId + 1;
        return stateId;
    }

    public SessionPhase phase() {
        return phase;
    }

    public void setPhase(SessionPhase phase) {
        this.phase = phase == null ? SessionPhase.IDLE : phase;
    }

    public int generation() {
        return generation;
    }

    public int bumpGeneration() {
        generation = generation == Integer.MAX_VALUE ? 1 : generation + 1;
        return generation;
    }

    public long lastClickNanos() {
        return lastClickNanos;
    }

    public void markClick(long nanos) {
        this.lastClickNanos = nanos;
    }

    public Component title() {
        return title;
    }

    public void setTitle(Component title) {
        this.title = title == null ? Component.empty() : title;
    }

    public int topSlotCount() {
        return menu.type().protocolTopSize();
    }

    public InventoryType type() {
        return menu.type();
    }

    public SlotKind slotKind(int slot) {
        if (slot < 0 || slot > menu.type().protocolLastIndex()) {
            return SlotKind.DECORATIVE;
        }
        Button button = menu.buttons().get(slot);
        if (button != null) {
            return button.kind();
        }
        return menu.isEditable() ? SlotKind.EDITABLE : SlotKind.DECORATIVE;
    }

    public boolean isEditableSlot(int slot) {
        return slotKind(slot) == SlotKind.EDITABLE;
    }
}
