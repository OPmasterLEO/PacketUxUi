package net.opmasterleo.packetuxui.event;

/**
 * Lightweight InventoryDragEvent analogue — no Bukkit event bus.
 * Prefer {@link net.opmasterleo.packetuxui.PacketMenus#onInventoryDrag(GuiDragListener)}
 * when you only care about drags.
 */
@FunctionalInterface
public interface GuiDragListener {

    void onInventoryDrag(GuiDragEvent event);
}
