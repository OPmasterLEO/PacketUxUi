package net.opmasterleo.packetuxui.event;

import org.bukkit.entity.Player;

import net.opmasterleo.packetuxui.service.Menu;

public final class GuiOpenEvent extends GuiEvent {

    public GuiOpenEvent(Player player, Menu menu, int windowId, int topSlotCount) {
        super(player, menu, windowId, topSlotCount);
    }
}
