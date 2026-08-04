package net.opmasterleo.packetuxui.nms.shared;

import net.minecraft.SharedConstants;
import net.opmasterleo.packetuxui.nms.ItemBridge;
import net.opmasterleo.packetuxui.nms.MenuPacketBridge;
import net.opmasterleo.packetuxui.nms.NmsAdapter;
import net.opmasterleo.packetuxui.nms.PacketClassifier;
import net.opmasterleo.packetuxui.nms.PipelineBridge;
import net.opmasterleo.packetuxui.nms.ProtocolVersions;

public abstract class AbstractNmsAdapter implements NmsAdapter {

    private final String bucketId;
    private final int minProtocol;
    private final int maxProtocol;
    private final SharedItemBridge items;
    private final SharedMenuPacketBridge packets;
    private final SharedPipelineBridge pipeline;
    private final SharedPacketClassifier classifier;

    protected AbstractNmsAdapter(String bucketId, int minProtocol, int maxProtocol) {
        int protocol = SharedConstants.getProtocolVersion();
        ProtocolVersions.requireInRange(protocol, minProtocol, maxProtocol, bucketId);
        this.bucketId = bucketId;
        this.minProtocol = minProtocol;
        this.maxProtocol = maxProtocol;
        this.items = new SharedItemBridge();
        this.packets = new SharedMenuPacketBridge(items);
        this.pipeline = new SharedPipelineBridge();
        this.classifier = new SharedPacketClassifier(items);
    }

    @Override
    public String bucketId() {
        return bucketId;
    }

    @Override
    public int minProtocol() {
        return minProtocol;
    }

    @Override
    public int maxProtocol() {
        return maxProtocol;
    }

    @Override
    public MenuPacketBridge packets() {
        return packets;
    }

    @Override
    public PipelineBridge pipeline() {
        return pipeline;
    }

    @Override
    public PacketClassifier classifier() {
        return classifier;
    }

    @Override
    public ItemBridge items() {
        return items;
    }
}
