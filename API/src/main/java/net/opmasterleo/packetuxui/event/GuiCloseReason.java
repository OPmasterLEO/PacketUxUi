package net.opmasterleo.packetuxui.event;

/**
 * Why a virtual menu closed — richer than Bukkit {@code InventoryCloseEvent} (no reason there).
 */
public enum GuiCloseReason {
    PLAYER,
    API,
    REPLACE,
    DEATH,
    KICK,
    QUIT,
    UNKNOWN
}
