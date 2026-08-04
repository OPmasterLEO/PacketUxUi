package net.opmasterleo.packetuxui.event;

/**
 * Packet analogue of {@code InventoryType.SlotType} for virtual menus.
 */
public enum GuiSlotType {
    /** Top / container slots (virtual menu). */
    CONTAINER,
    /** Player inventory storage (slots top..top+26). */
    PLAYER,
    /** Hotbar (slots top+27..top+35). */
    HOTBAR,
    /** Outside click (-999) or invalid. */
    OUTSIDE
}
