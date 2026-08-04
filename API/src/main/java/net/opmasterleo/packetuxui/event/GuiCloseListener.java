package net.opmasterleo.packetuxui.event;

/**
 * Lightweight InventoryCloseEvent analogue — no Bukkit event bus.
 * Prefer {@link net.opmasterleo.packetuxui.PacketMenus#onInventoryClose(GuiCloseListener)}
 * when you only care about closes.
 */
@FunctionalInterface
public interface GuiCloseListener {

    void onInventoryClose(GuiCloseEvent event);
}
