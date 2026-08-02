package net.opmasterleo.packetuxui.nms.v1_8_R3;

import net.opmasterleo.packetuxui.nms.ProtocolVersions;
import net.opmasterleo.packetuxui.nms.v1_8_R3.shared.AbstractNmsAdapter;

public final class Adapter extends AbstractNmsAdapter {
    public Adapter() {
        super("v1_8_R3", "v1_8_R3", ProtocolVersions.R1_8_R3_MIN, ProtocolVersions.R1_8_R3_MAX);
    }
}