package net.opmasterleo.packetuxui.service;

public final class MenuSession {

    private final Menu menu;
    private final int windowId;
    private int stateId;

    public MenuSession(Menu menu, int windowId) {
        this.menu = menu;
        this.windowId = windowId;
        this.stateId = 0;
    }

    public Menu menu() {
        return menu;
    }

    public int windowId() {
        return windowId;
    }

    public int stateId() {
        return stateId;
    }

    public int nextStateId() {
        stateId = stateId == Integer.MAX_VALUE ? 1 : stateId + 1;
        return stateId;
    }
}
