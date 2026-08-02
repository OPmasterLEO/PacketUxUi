package net.opmasterleo.packetuxui.types;

import java.util.function.Consumer;

import org.bukkit.entity.Player;

import net.opmasterleo.packetuxui.nms.item.UxItem;

public final class ExecuteComponent {

    private final Player player;
    private final ButtonType buttonType;
    private final int slot;
    private final UxItem itemStack;

    public ExecuteComponent(Player player, ButtonType buttonType, int slot, UxItem itemStack) {
        this.player = player;
        this.buttonType = buttonType;
        this.slot = slot;
        this.itemStack = itemStack;
    }

    public Player player() {
        return player;
    }

    public ButtonType buttonType() {
        return buttonType;
    }

    public int slot() {
        return slot;
    }

    public UxItem itemStack() {
        return itemStack;
    }

    @FunctionalInterface
    public interface Handler extends Consumer<ExecuteComponent> {
    }
}
