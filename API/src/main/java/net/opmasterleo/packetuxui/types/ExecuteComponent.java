package net.opmasterleo.packetuxui.types;

import java.util.function.Consumer;

import org.bukkit.entity.Player;

import net.opmasterleo.packetuxui.nms.item.UxItem;

public final class ExecuteComponent {

    private final Player player;
    private final ButtonType buttonType;
    private final ClickType clickType;
    private final int slot;
    private final UxItem itemStack;
    private final UxItem carried;
    private final int topSlotCount;

    public ExecuteComponent(Player player, ButtonType buttonType, int slot, UxItem itemStack) {
        this(player, buttonType, ClickType.UNDEFINED, slot, itemStack, UxItem.EMPTY, -1);
    }

    public ExecuteComponent(
            Player player,
            ButtonType buttonType,
            ClickType clickType,
            int slot,
            UxItem itemStack,
            UxItem carried,
            int topSlotCount
    ) {
        this.player = player;
        this.buttonType = buttonType == null ? ButtonType.LEFT : buttonType;
        this.clickType = clickType == null ? ClickType.UNDEFINED : clickType;
        this.slot = slot;
        this.itemStack = itemStack == null ? UxItem.EMPTY : itemStack;
        this.carried = carried == null ? UxItem.EMPTY : carried;
        this.topSlotCount = topSlotCount;
    }

    public Player player() {
        return player;
    }

    public ButtonType buttonType() {
        return buttonType;
    }

    public ClickType clickType() {
        return clickType;
    }

    public int slot() {
        return slot;
    }

    /** Item in the clicked top slot (or empty). */
    public UxItem itemStack() {
        return itemStack;
    }

    /** Cursor / carried item at handle time. */
    public UxItem carried() {
        return carried;
    }

    public int topSlotCount() {
        return topSlotCount;
    }

    public boolean isTop() {
        return topSlotCount >= 0 && slot >= 0 && slot < topSlotCount;
    }

    public boolean isBottom() {
        return topSlotCount >= 0 && slot >= topSlotCount && slot < topSlotCount + 36;
    }

    @FunctionalInterface
    public interface Handler extends Consumer<ExecuteComponent> {
    }
}
