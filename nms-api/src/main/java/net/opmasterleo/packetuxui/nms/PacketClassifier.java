package net.opmasterleo.packetuxui.nms;

public interface PacketClassifier {

    enum Kind {
        OTHER,
        CLICK,
        CLOSE
    }

    Kind kindOf(Object packet);

    ClickPacket readClick(Object packet);

    boolean isClose(Object packet);
}
