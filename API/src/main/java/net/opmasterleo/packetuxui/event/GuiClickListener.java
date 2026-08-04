package net.opmasterleo.packetuxui.event;

/**
 * Lightweight InventoryClickEvent analogue — no Bukkit event bus.
 * Prefer {@link net.opmasterleo.packetuxui.PacketMenus#onInventoryClick(GuiClickListener)}
 * when you only care about clicks.
 */
@FunctionalInterface
public interface GuiClickListener {

    void onInventoryClick(GuiClickEvent event);
}
