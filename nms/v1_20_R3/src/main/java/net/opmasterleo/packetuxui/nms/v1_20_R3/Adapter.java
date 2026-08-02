package net.opmasterleo.packetuxui.nms.v1_20_R3;

import net.opmasterleo.packetuxui.nms.ProtocolVersions;
import net.opmasterleo.packetuxui.nms.v1_20_R3.shared.AbstractNmsAdapter;

public final class Adapter extends AbstractNmsAdapter {
    public Adapter() {
        super("v1_20_R3", ProtocolVersions.R1_20_R3_MIN, ProtocolVersions.R1_20_R3_MAX);
    }
}