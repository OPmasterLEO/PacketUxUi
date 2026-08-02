package net.opmasterleo.packetuxui.nms.v1_9_R2;

import net.opmasterleo.packetuxui.nms.ProtocolVersions;
import net.opmasterleo.packetuxui.nms.v1_9_R2.shared.AbstractNmsAdapter;

public final class Adapter extends AbstractNmsAdapter {
    public Adapter() {
        super("v1_9_R2", "v1_9_R2", ProtocolVersions.R1_9_R2_MIN, ProtocolVersions.R1_9_R2_MAX);
    }
}