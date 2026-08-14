package net.opmasterleo.packetuxui.types;

public final class ClickData {

    public static final ClickData LEFT_PICKUP = new ClickData(ButtonType.LEFT, ClickType.PICKUP);
    public static final ClickData LEFT_PLACE = new ClickData(ButtonType.LEFT, ClickType.PLACE);
    public static final ClickData RIGHT_PICKUP = new ClickData(ButtonType.RIGHT, ClickType.PICKUP);
    public static final ClickData RIGHT_PLACE = new ClickData(ButtonType.RIGHT, ClickType.PLACE);
    public static final ClickData SHIFT_LEFT = new ClickData(ButtonType.SHIFT_LEFT, ClickType.SHIFT_CLICK);
    public static final ClickData SHIFT_RIGHT = new ClickData(ButtonType.SHIFT_RIGHT, ClickType.SHIFT_CLICK);
    public static final ClickData MIDDLE_PICKUP = new ClickData(ButtonType.MIDDLE, ClickType.PICKUP);
    public static final ClickData DROP_PICKUP = new ClickData(ButtonType.DROP, ClickType.PICKUP);
    public static final ClickData CTRL_DROP_PICKUP = new ClickData(ButtonType.CTRL_DROP, ClickType.PICKUP);
    public static final ClickData F_PICKUP = new ClickData(ButtonType.F, ClickType.PICKUP);
    public static final ClickData DOUBLE_CLICK = new ClickData(ButtonType.DOUBLE_CLICK, ClickType.PICKUP_ALL);
    public static final ClickData LEFT_UNDEFINED = new ClickData(ButtonType.LEFT, ClickType.UNDEFINED);
    public static final ClickData LEFT_DRAG_START = new ClickData(ButtonType.LEFT, ClickType.DRAG_START);
    public static final ClickData RIGHT_DRAG_START = new ClickData(ButtonType.RIGHT, ClickType.DRAG_START);
    public static final ClickData MIDDLE_DRAG_START = new ClickData(ButtonType.MIDDLE, ClickType.DRAG_START);
    public static final ClickData LEFT_DRAG_ADD = new ClickData(ButtonType.LEFT, ClickType.DRAG_ADD);
    public static final ClickData RIGHT_DRAG_ADD = new ClickData(ButtonType.RIGHT, ClickType.DRAG_ADD);
    public static final ClickData MIDDLE_DRAG_ADD = new ClickData(ButtonType.MIDDLE, ClickType.DRAG_ADD);
    public static final ClickData LEFT_DRAG_END = new ClickData(ButtonType.LEFT, ClickType.DRAG_END);
    public static final ClickData RIGHT_DRAG_END = new ClickData(ButtonType.RIGHT, ClickType.DRAG_END);
    public static final ClickData MIDDLE_DRAG_END = new ClickData(ButtonType.MIDDLE, ClickType.DRAG_END);
    /** Precomputed hotbar-swap clicks (SWAP button 0..8) — no per-click allocation. */
    public static final ClickData[] HOTBAR_SWAP = {
            new ClickData(ButtonType.NUM_1, ClickType.PICKUP),
            new ClickData(ButtonType.NUM_2, ClickType.PICKUP),
            new ClickData(ButtonType.NUM_3, ClickType.PICKUP),
            new ClickData(ButtonType.NUM_4, ClickType.PICKUP),
            new ClickData(ButtonType.NUM_5, ClickType.PICKUP),
            new ClickData(ButtonType.NUM_6, ClickType.PICKUP),
            new ClickData(ButtonType.NUM_7, ClickType.PICKUP),
            new ClickData(ButtonType.NUM_8, ClickType.PICKUP),
            new ClickData(ButtonType.NUM_9, ClickType.PICKUP)
    };

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
