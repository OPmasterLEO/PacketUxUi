package net.opmasterleo.packetuxui.nms.v26_2;

import net.opmasterleo.packetuxui.nms.ProtocolVersions;
import net.opmasterleo.packetuxui.nms.v26_2.shared.AbstractNmsAdapter;

public final class Adapter extends AbstractNmsAdapter {
    public Adapter() {
        super("v26_2", ProtocolVersions.R26_2_MIN, ProtocolVersions.R26_2_MAX);
    }
}