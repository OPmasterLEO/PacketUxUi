package net.opmasterleo.packetuxui.event;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.opmasterleo.packetuxui.PacketUxUiAPI;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.types.ButtonType;
import net.opmasterleo.packetuxui.types.ClickData;
import net.opmasterleo.packetuxui.types.ClickType;

/**
 * Packet analogue of Bukkit {@code InventoryClickEvent}.
 * Cancel to skip PacketUxUi handlers and resync the client.
 * Prefer {@link net.opmasterleo.packetuxui.PacketMenus#onInventoryClick} for a dedicated hook.
 */
public final class GuiClickEvent extends GuiEvent {

    private final GuiView view;
    private final ClickPacket packet;
    private final ClickData clickData;
    private final ClickType clickType;
    private final ButtonType buttonType;
    private final GuiClickAction action;
    private final GuiSlotType slotType;
    private final int slot;
    private final int hotbarButton;
    private final UxItem currentItem;
    private final UxItem cursor;
    private boolean cancelled;

    public GuiClickEvent(
            Player player,
            Menu menu,
            int windowId,
            int topSlotCount,
            int stateId,
            ClickPacket packet,
            ClickData clickData,
            UxItem cursor
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
                menu == null ? net.opmasterleo.packetuxui.nms.LiveLimits.playerInventorySlots() : menu.type().bottomSlotCount()
        );
        this.currentItem = GuiClickMapper.currentItem(menu, this.slot, topSlotCount);
        this.cursor = cursor == null ? UxItem.EMPTY : cursor;
        this.action = GuiClickMapper.action(packet, clickData, this.currentItem, this.cursor);
        this.hotbarButton = GuiClickMapper.hotbarButton(packet, this.buttonType);
    }

    /** {@code InventoryView}-like snapshot. */
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

    /** Bukkit {@code InventoryAction} analogue. */
    public GuiClickAction action() {
        return action;
    }

    /** Bukkit {@code SlotType} analogue. */
    public GuiSlotType slotType() {
        return slotType;
    }

    public int slot() {
        return slot;
    }

    /** Raw/protocol slot (same as {@link #slot()} for packet menus). */
    public int rawSlot() {
        return slot;
    }

    /** Hotbar key 0..8 for number-key swap, else -1. */
    public int hotbarButton() {
        return hotbarButton;
    }

    public boolean isTop() {
        return slotType == GuiSlotType.CONTAINER;
    }

    public boolean isBottom() {
        return slotType == GuiSlotType.PLAYER || slotType == GuiSlotType.HOTBAR;
    }

    public boolean isOutside() {
        return slotType == GuiSlotType.OUTSIDE;
    }

    /** Item in the clicked top slot (empty for bottom/outside). */
    public UxItem currentItem() {
        return currentItem;
    }

    /** Cursor / carried at event time. */
    public UxItem cursor() {
        return cursor;
    }

    /**
     * Bukkit {@link ItemStack} for {@link #currentItem()} (uses native clone when present).
     * Prefer {@link #currentItem()} on hot paths.
     */
    public ItemStack currentStack() {
        return toBukkit(currentItem);
    }

    /**
     * Bukkit {@link ItemStack} for {@link #cursor()}.
     * Prefer {@link #cursor()} on hot paths.
     */
    public ItemStack cursorStack() {
        return toBukkit(cursor);
    }

    /** @deprecated use {@link #cursor()} */
    @Deprecated
    public UxItem carried() {
        return cursor;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    private static ItemStack toBukkit(UxItem item) {
        if (item == null || item.isEmpty()) {
            return new ItemStack(Material.AIR);
        }
        try {
            return PacketUxUiAPI.getAdapter().items().toBukkit(item);
        } catch (Throwable ignored) {
            return new ItemStack(Material.AIR);
        }
    }
}
