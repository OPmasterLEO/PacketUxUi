package net.opmasterleo.packetuxui.dto;

import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.types.ClickType;

public final class AccumulatedDrag {

    private final ClickPacket packet;
    private final ClickType type;

    public AccumulatedDrag(ClickPacket packet, ClickType type) {
        this.packet = packet;
        this.type = type;
    }

    public ClickPacket packet() {
        return packet;
    }

    public ClickType type() {
        return type;
    }
}
