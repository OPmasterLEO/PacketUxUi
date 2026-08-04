package net.opmasterleo.packetuxui.event;

import org.bukkit.entity.Player;

import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.types.ButtonType;
import net.opmasterleo.packetuxui.types.ClickData;
import net.opmasterleo.packetuxui.types.ClickType;

/** Fired after PacketUxUi finished handling a click. */
public final class GuiClickPostEvent extends GuiEvent {

    private final GuiView view;
    private final ClickPacket packet;
    private final ClickData clickData;
    private final ClickType clickType;
    private final ButtonType buttonType;
    private final GuiClickAction action;
    private final GuiSlotType slotType;
    private final int slot;
    private final UxItem currentItem;
    private final UxItem cursor;
    private final String decision;

    public GuiClickPostEvent(
            Player player,
            Menu menu,
            int windowId,
            int topSlotCount,
            int stateId,
            ClickPacket packet,
            ClickData clickData,
            UxItem cursor,
            String decision
    ) {
        super(player, menu, windowId, topSlotCount);
        this.view = new GuiView(windowId, menu, topSlotCount, stateId);
        this.packet = packet;
        this.clickData = clickData;
        this.clickType = clickData == null || clickData.clickType() == null
                ? ClickType.UNDEFINED
                : clickData.clickType();
        this.buttonType = clickData == null || clickData.buttonType() == null
                ? ButtonType.LEFT
                : clickData.buttonType();
        this.slot = packet.slot();
        this.slotType = GuiClickMapper.slotType(
                this.slot,
                topSlotCount,
                menu == null ? 36 : menu.type().bottomSlotCount()
        );
        this.currentItem = GuiClickMapper.currentItem(menu, this.slot, topSlotCount);
        this.cursor = cursor == null ? UxItem.EMPTY : cursor;
        this.action = GuiClickMapper.action(packet, clickData, this.currentItem, this.cursor);
        this.decision = decision == null ? "" : decision;
    }

    public GuiView view() {
        return view;
    }

    public ClickPacket packet() {
        return packet;
    }

    public ClickData clickData() {
        return clickData;
    }

    public ClickType clickType() {
        return clickType;
    }

    public ButtonType buttonType() {
        return buttonType;
    }

    public GuiClickAction action() {
        return action;
    }

    public GuiSlotType slotType() {
        return slotType;
    }

    public int slot() {
        return slot;
    }

    public boolean isTop() {
        return slotType == GuiSlotType.CONTAINER;
    }

    public UxItem currentItem() {
        return currentItem;
    }

    public UxItem cursor() {
        return cursor;
    }

    /** @deprecated use {@link #cursor()} */
    @Deprecated
    public UxItem carried() {
        return cursor;
    }

    public String decision() {
        return decision;
    }
}
