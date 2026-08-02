package net.opmasterleo.packetuxui.service;

public enum MenuMode {
    /**
     * Virtual top inventory only. All clicks are consumed; dirty slots are
     * reverted with Set Slot packets. No {@code updateInventory}, no inject.
     */
    READ_ONLY,

    /**
     * Virtual top inventory; clicks on the player inventory strip under the
     * menu are forwarded into container 0. Top slots stay read-only UI.
     * Prefer Bukkit EditableGui for true item ownership (sell/order/auction).
     */
    EDITABLE_PLAYER_INVENTORY
}
