package net.opmasterleo.packetuxui.manager;

public record PresentOptions(
        boolean forceReopenIfOnCloseChanged
) {
    public static final PresentOptions DEFAULT = new PresentOptions(false);
    public static final PresentOptions FORCE_REOPEN_ON_CLOSE_CHANGE = new PresentOptions(true);
}
