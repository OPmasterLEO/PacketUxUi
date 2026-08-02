package net.opmasterleo.packetuxui.types;

public final class ClickData {

    private final ButtonType buttonType;
    private final ClickType clickType;

    public ClickData(ButtonType buttonType, ClickType clickType) {
        this.buttonType = buttonType;
        this.clickType = clickType;
    }

    public ButtonType buttonType() {
        return buttonType;
    }

    public ClickType clickType() {
        return clickType;
    }
}
