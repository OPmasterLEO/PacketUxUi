package net.opmasterleo.packetuxui.event;

/**
 * Drag phase — packet analogue of {@code InventoryDragEvent} lifecycle
 * (Bukkit collapses START/ADD/END into one event after the fact).
 */
public enum GuiDragPhase {
    START,
    ADD,
    END
}
