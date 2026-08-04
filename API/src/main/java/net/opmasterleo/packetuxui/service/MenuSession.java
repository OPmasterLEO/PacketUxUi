package net.opmasterleo.packetuxui.service;

import java.util.Objects;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.types.InventoryType;

public final class MenuSession {

    private Menu menu;
    private final int windowId;
    /** Last stateId sent to the client (mirrors NMS when bound). */
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

    /**
     * Swap the open menu without changing window id / phase (type-change present).
     */
    public void replaceMenu(Menu menu) {
        this.menu = Objects.requireNonNull(menu, "menu");
        this.title = menu.name();
        bumpGeneration();
    }

    public int windowId() {
        return windowId;
    }

    public int stateId() {
        return stateId;
    }

    /** Record the last protocol stateId sent (from NMS bump or local counter). */
    public void recordStateId(int stateId) {
        this.stateId = stateId;
    }

    /**
     * Local fallback counter for adapters without NMS {@code incrementStateId}.
     * Prefer {@link net.opmasterleo.packetuxui.nms.MenuPacketBridge#bumpStateId} when available.
     */
    public int nextStateId() {
        stateId = stateId == Integer.MAX_VALUE ? 1 : stateId + 1;
        return stateId;
    }

    /**
     * Bump past both our last state and the client's click stateId so correction packets
     * are accepted after optimistic client-side inventory mutations.
     */
    public int nextStateIdAbove(int clientStateId) {
        int floor = Math.max(stateId, Math.max(0, clientStateId));
        stateId = floor == Integer.MAX_VALUE ? 1 : floor + 1;
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

    public boolean isExtractableSlot(int slot) {
        return slotKind(slot) == SlotKind.EXTRACTABLE;
    }

    public boolean allowsTake(int slot) {
        SlotKind kind = slotKind(slot);
        return kind == SlotKind.EDITABLE || kind == SlotKind.EXTRACTABLE;
    }

    public boolean allowsPlace(int slot) {
        return slotKind(slot) == SlotKind.EDITABLE;
    }
}
