package net.opmasterleo.packetuxui.event;

import org.bukkit.entity.Player;

import net.opmasterleo.packetuxui.service.Menu;

/** Packet analogue of Bukkit {@code InventoryOpenEvent}. */
public final class GuiOpenEvent extends GuiEvent {

    private final GuiView view;

    public GuiOpenEvent(Player player, Menu menu, int windowId, int topSlotCount, int stateId) {
        super(player, menu, windowId, topSlotCount);
        this.view = new GuiView(windowId, menu, topSlotCount, stateId);
    }

    public GuiView view() {
        return view;
    }
}
