package net.opmasterleo.packetuxui.event;

/**
 * Why a packet menu screen was shown to the client.
 */
public enum GuiOpenReason {
    /** Fresh open ({@code open}/{@code present} with no prior session). */
    OPEN,
    /** Silent type/size/mode swap — OpenScreen only, same window id. */
    TYPE_SWAP
}
