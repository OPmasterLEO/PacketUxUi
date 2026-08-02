package net.opmasterleo.packetuxui.nms.v1_20_R4;

import net.opmasterleo.packetuxui.nms.ProtocolVersions;
import net.opmasterleo.packetuxui.nms.v1_20_R4.shared.AbstractNmsAdapter;

public final class Adapter extends AbstractNmsAdapter {
    public Adapter() {
        super("v1_20_R4", ProtocolVersions.R1_20_R4_MIN, ProtocolVersions.R1_20_R4_MAX);
    }
}