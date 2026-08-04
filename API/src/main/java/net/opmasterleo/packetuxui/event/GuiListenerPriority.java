package net.opmasterleo.packetuxui.event;

/**
 * Listener priority. Lower ordinal runs first for pre-click;
 * higher ordinal runs first for post-click (reverse).
 */
public enum GuiListenerPriority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST,
    MONITOR
}
