package net.opmasterleo.packetuxui.nms.v26_1;

import net.opmasterleo.packetuxui.nms.ProtocolVersions;
import net.opmasterleo.packetuxui.nms.v26_1.shared.AbstractNmsAdapter;

public final class Adapter extends AbstractNmsAdapter {
    public Adapter() {
        super("v26_1", ProtocolVersions.R26_1_MIN, ProtocolVersions.R26_1_MAX);
    }
}