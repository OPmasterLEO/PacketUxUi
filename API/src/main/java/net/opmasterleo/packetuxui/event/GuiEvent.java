package net.opmasterleo.packetuxui.event;

import org.bukkit.entity.Player;

import net.opmasterleo.packetuxui.service.Menu;

public abstract class GuiEvent {

    private final Player player;
    private final Menu menu;
    private final int windowId;
    private final int topSlotCount;

    protected GuiEvent(Player player, Menu menu, int windowId, int topSlotCount) {
        this.player = player;
        this.menu = menu;
        this.windowId = windowId;
        this.topSlotCount = topSlotCount;
    }

    public Player player() {
        return player;
    }

    public Menu menu() {
        return menu;
    }

    public int windowId() {
        return windowId;
    }

    public int topSlotCount() {
        return topSlotCount;
    }
}
