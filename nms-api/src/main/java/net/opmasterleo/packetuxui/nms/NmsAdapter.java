package net.opmasterleo.packetuxui.nms;

public interface NmsAdapter {

    String bucketId();

    int minProtocol();

    int maxProtocol();

    MenuPacketBridge packets();

    PipelineBridge pipeline();

    PacketClassifier classifier();

    ItemBridge items();

    /** Vanilla limits sourced from NMS on this server (books, inv size, window ids, …). */
    default ServerLimits limits() {
        return ServerLimits.FALLBACK;
    }

    default SignPacketBridge signs() {
        return SignPacketBridge.UNSUPPORTED;
    }
}
