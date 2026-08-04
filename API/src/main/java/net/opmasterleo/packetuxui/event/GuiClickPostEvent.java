package net.opmasterleo.packetuxui.event;

import org.bukkit.entity.Player;

import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.types.ButtonType;
import net.opmasterleo.packetuxui.types.ClickType;

/** Fired after PacketUxUi finished handling a click (handler ran or settle completed). */
public final class GuiClickPostEvent extends GuiEvent {

    private final ClickPacket packet;
    private final ClickType clickType;
    private final ButtonType buttonType;
    private final int slot;
    private final boolean top;
    private final UxItem carried;
    private final String decision;

    public GuiClickPostEvent(
            Player player,
            Menu menu,
            int windowId,
            int topSlotCount,
            ClickPacket packet,
            ClickType clickType,
            ButtonType buttonType,
            UxItem carried,
            String decision
    ) {
        super(player, menu, windowId, topSlotCount);
        this.packet = packet;
        this.clickType = clickType == null ? ClickType.UNDEFINED : clickType;
        this.buttonType = buttonType == null ? ButtonType.LEFT : buttonType;
        this.slot = packet.slot();
        this.top = this.slot >= 0 && this.slot < topSlotCount;
        this.carried = carried == null ? UxItem.EMPTY : carried;
        this.decision = decision == null ? "" : decision;
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

    public UxItem carried() {
        return carried;
    }

    /** Last internal decision tag (debug / observability). */
    public String decision() {
        return decision;
    }
}
