package net.opmasterleo.packetuxui.event;

import org.bukkit.entity.Player;

import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.types.ButtonType;
import net.opmasterleo.packetuxui.types.ClickType;

/**
 * Fired before PacketUxUi handles a virtual-menu click. Cancel to skip handlers and settle
 * the client (resync). Plugins use this for combat locks, staff filters, etc.
 */
public final class GuiClickEvent extends GuiEvent {

    private final ClickPacket packet;
    private final ClickType clickType;
    private final ButtonType buttonType;
    private final int slot;
    private final boolean top;
    private final UxItem carried;
    private boolean cancelled;

    public GuiClickEvent(
            Player player,
            Menu menu,
            int windowId,
            int topSlotCount,
            ClickPacket packet,
            ClickType clickType,
            ButtonType buttonType,
            UxItem carried
    ) {
        super(player, menu, windowId, topSlotCount);
        this.packet = packet;
        this.clickType = clickType == null ? ClickType.UNDEFINED : clickType;
        this.buttonType = buttonType == null ? ButtonType.LEFT : buttonType;
        this.slot = packet.slot();
        this.top = this.slot >= 0 && this.slot < topSlotCount;
        this.carried = carried == null ? UxItem.EMPTY : carried;
    }

    public ClickPacket packet() {
        return packet;
    }

    public ClickType clickType() {
        return clickType;
    }

    public ButtonType buttonType() {
        return buttonType;
    }

    public int slot() {
        return slot;
    }

    public boolean isTop() {
        return top;
    }

    public boolean isBottom() {
        return slot >= topSlotCount() && slot < topSlotCount() + 36;
    }

    public UxItem carried() {
        return carried;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
