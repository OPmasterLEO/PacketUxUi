package net.opmasterleo.packetuxui.event;

import org.bukkit.entity.Player;

import net.opmasterleo.packetuxui.service.Menu;

public final class GuiCloseEvent extends GuiEvent {

    public GuiCloseEvent(Player player, Menu menu, int windowId, int topSlotCount) {
        super(player, menu, windowId, topSlotCount);
    }
}
