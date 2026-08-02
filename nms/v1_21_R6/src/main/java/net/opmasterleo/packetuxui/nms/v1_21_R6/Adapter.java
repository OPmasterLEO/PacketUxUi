package net.opmasterleo.packetuxui.nms.v1_21_R6;

import net.opmasterleo.packetuxui.nms.ProtocolVersions;
import net.opmasterleo.packetuxui.nms.v1_21_R6.shared.AbstractNmsAdapter;

public final class Adapter extends AbstractNmsAdapter {
    public Adapter() {
        super("v1_21_R6", ProtocolVersions.R1_21_R6_MIN, ProtocolVersions.R1_21_R6_MAX);
    }
}