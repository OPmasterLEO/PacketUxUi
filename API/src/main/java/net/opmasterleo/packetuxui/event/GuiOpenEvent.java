package net.opmasterleo.packetuxui.event;

import org.bukkit.entity.Player;

import net.opmasterleo.packetuxui.service.Menu;

/**
 * Packet-native InventoryOpenEvent — direct listener call, no Bukkit {@code HandlerList}.
 * Fired on fresh opens and silent type-swaps ({@link GuiOpenReason}).
 */
public final class GuiOpenEvent extends GuiEvent {

    private final GuiView view;
    private final GuiOpenReason reason;

    public GuiOpenEvent(Player player, Menu menu, int windowId, int topSlotCount, int stateId) {
        this(player, menu, windowId, topSlotCount, stateId, GuiOpenReason.OPEN);
    }

    public GuiOpenEvent(
            Player player,
            Menu menu,
            int windowId,
            int topSlotCount,
            int stateId,
            GuiOpenReason reason
    ) {
        super(player, menu, windowId, topSlotCount);
        this.view = new GuiView(windowId, menu, topSlotCount, stateId);
        this.reason = reason == null ? GuiOpenReason.OPEN : reason;
    }

    public GuiView view() {
        return view;
    }

    /** Fresh {@link GuiOpenReason#OPEN} or silent {@link GuiOpenReason#TYPE_SWAP}. */
    public GuiOpenReason reason() {
        return reason;
    }

    public boolean isTypeSwap() {
        return reason == GuiOpenReason.TYPE_SWAP;
    }
}
