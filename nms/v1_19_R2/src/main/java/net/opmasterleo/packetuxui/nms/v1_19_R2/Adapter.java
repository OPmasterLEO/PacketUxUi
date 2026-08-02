package net.opmasterleo.packetuxui.nms.v1_19_R2;

import net.opmasterleo.packetuxui.nms.ProtocolVersions;
import net.opmasterleo.packetuxui.nms.v1_19_R2.shared.AbstractNmsAdapter;

public final class Adapter extends AbstractNmsAdapter {
    public Adapter() {
        super("v1_19_R2", ProtocolVersions.R1_19_R2_MIN, ProtocolVersions.R1_19_R2_MAX);
    }
}