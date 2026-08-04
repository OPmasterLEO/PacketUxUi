package net.opmasterleo.packetuxui.event;

/**
 * Packet analogue of Bukkit {@code InventoryAction} — what the click <em>means</em>.
 * Informational for listeners; cancel {@link GuiClickEvent} to override.
 */
public enum GuiClickAction {
    NOTHING,
    PICKUP_ALL,
    PICKUP_HALF,
    PICKUP_ONE,
    PLACE_ALL,
    PLACE_ONE,
    SWAP_WITH_CURSOR,
    DROP_ALL_CURSOR,
    DROP_ONE_CURSOR,
    DROP_ALL_SLOT,
    DROP_ONE_SLOT,
    MOVE_TO_OTHER_INVENTORY,
    HOTBAR_SWAP,
    CLONE_STACK,
    COLLECT_TO_CURSOR,
    UNKNOWN
}
