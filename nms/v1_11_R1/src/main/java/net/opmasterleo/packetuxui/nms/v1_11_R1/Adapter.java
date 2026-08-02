package net.opmasterleo.packetuxui.nms.v1_11_R1;

import net.opmasterleo.packetuxui.nms.ProtocolVersions;
import net.opmasterleo.packetuxui.nms.v1_11_R1.shared.AbstractNmsAdapter;

public final class Adapter extends AbstractNmsAdapter {
    public Adapter() {
        super("v1_11_R1", "v1_11_R1", ProtocolVersions.R1_11_R1_MIN, ProtocolVersions.R1_11_R1_MAX);
    }
}