package net.opmasterleo.packetuxui.nms;

public final class NmsUnsupportedException extends RuntimeException {

    public NmsUnsupportedException(String message) {
        super(message);
    }

    public NmsUnsupportedException(String message, Throwable cause) {
        super(message, cause);
    }
}
