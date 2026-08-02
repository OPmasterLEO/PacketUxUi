package net.opmasterleo.packetuxui.nms.v1_21_R5;

import net.opmasterleo.packetuxui.nms.ProtocolVersions;
import net.opmasterleo.packetuxui.nms.v1_21_R5.shared.AbstractNmsAdapter;

public final class Adapter extends AbstractNmsAdapter {
    public Adapter() {
        super("v1_21_R5", ProtocolVersions.R1_21_R5_MIN, ProtocolVersions.R1_21_R5_MAX);
    }
}