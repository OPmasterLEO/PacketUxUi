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

    default int clickWindowId(Object packet) {
        ClickPacket click = readClick(packet);
        return click == null ? -1 : click.windowId();
    }

    /**
     * Container id from a close packet, or {@code -1} when unknown / unsupported on this platform.
     */
    default int closeWindowId(Object packet) {
        return -1;
    }
}
