package net.opmasterleo.packetuxui.event;

import org.bukkit.entity.Player;

import net.opmasterleo.packetuxui.service.CloseSnapshot;
import net.opmasterleo.packetuxui.service.Menu;

/** Packet analogue of Bukkit {@code InventoryCloseEvent}, with an explicit reason. */
public final class GuiCloseEvent extends GuiEvent {

    private final GuiView view;
    private final GuiCloseReason reason;
    private final CloseSnapshot snapshot;

    public GuiCloseEvent(
            Player player,
            Menu menu,
            int windowId,
            int topSlotCount,
            int stateId,
            GuiCloseReason reason,
            CloseSnapshot snapshot
    ) {
        super(player, menu, windowId, topSlotCount);
        this.view = new GuiView(windowId, menu, topSlotCount, stateId);
        this.reason = reason == null ? GuiCloseReason.UNKNOWN : reason;
        this.snapshot = snapshot;
    }

    public GuiView view() {
        return view;
    }

    public GuiCloseReason reason() {
        return reason;
    }

    /** Top items + cursor at close time; may be null. */
    public CloseSnapshot snapshot() {
        return snapshot;
    }
}
