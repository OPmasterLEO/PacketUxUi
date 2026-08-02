package net.opmasterleo.packetuxui.nms.v1_21_R7;

import net.opmasterleo.packetuxui.nms.ProtocolVersions;
import net.opmasterleo.packetuxui.nms.v1_21_R7.shared.AbstractNmsAdapter;

public final class Adapter extends AbstractNmsAdapter {
    public Adapter() {
        super("v1_21_R7", ProtocolVersions.R1_21_R7_MIN, ProtocolVersions.R1_21_R7_MAX);
    }
}