package net.opmasterleo.packetuxui.service;

public enum MenuMode {
    READ_ONLY,

    EDITABLE,

    /** @deprecated use {@link #EDITABLE} */
    @Deprecated
    EDITABLE_PLAYER_INVENTORY
}
