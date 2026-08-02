package net.opmasterleo.packetuxui.nms.v1_19_R3;

import net.opmasterleo.packetuxui.nms.ProtocolVersions;
import net.opmasterleo.packetuxui.nms.v1_19_R3.shared.AbstractNmsAdapter;

public final class Adapter extends AbstractNmsAdapter {
    public Adapter() {
        super("v1_19_R3", ProtocolVersions.R1_19_R3_MIN, ProtocolVersions.R1_19_R3_MAX);
    }
}