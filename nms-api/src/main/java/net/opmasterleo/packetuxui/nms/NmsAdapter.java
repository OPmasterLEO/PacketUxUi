package net.opmasterleo.packetuxui.nms;

public interface NmsAdapter {

    String bucketId();

    int minProtocol();

    int maxProtocol();

    MenuPacketBridge packets();

    PipelineBridge pipeline();

    PacketClassifier classifier();

    ItemBridge items();
}
