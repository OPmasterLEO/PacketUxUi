package net.opmasterleo.packetuxui.event;

import java.util.Set;

import org.bukkit.entity.Player;

import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.types.ButtonType;
import net.opmasterleo.packetuxui.types.ClickData;

/**
 * Packet analogue of Bukkit {@code InventoryDragEvent}, split by phase
 * (START / ADD / END) as the client sends QUICK_CRAFT packets.
 */
public final class GuiDragEvent extends GuiEvent {

    private final GuiView view;
    private final ClickPacket packet;
    private final GuiDragPhase phase;
    private final ButtonType buttonType;
    private final Set<Integer> slots;
    private final UxItem cursor;
    private boolean cancelled;

    public GuiDragEvent(
            Player player,
            Menu menu,
            int windowId,
            int topSlotCount,
            int stateId,
            ClickPacket packet,
            ClickData clickData,
            GuiDragPhase phase,
            Set<Integer> slots,
            UxItem cursor
    ) {
        super(player, menu, windowId, topSlotCount);
        this.view = new GuiView(windowId, menu, topSlotCount, stateId);
        this.packet = packet;
        this.phase = phase == null ? GuiDragPhase.ADD : phase;
        this.buttonType = clickData == null || clickData.buttonType() == null
                ? ButtonType.LEFT
                : clickData.buttonType();
        this.slots = slots == null ? Set.of() : Set.copyOf(slots);
        this.cursor = cursor == null ? UxItem.EMPTY : cursor;
    }

    public GuiView view() {
        return view;
    }

    public ClickPacket packet() {
        return packet;
    }

    public GuiDragPhase phase() {
        return phase;
    }

    public ButtonType buttonType() {
        return buttonType;
    }

    /** Slots involved in this drag packet (ADD/END); START may be empty. */
    public Set<Integer> slots() {
        return slots;
    }

    public UxItem cursor() {
        return cursor;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
