package net.opmasterleo.packetuxui.event;

/**
 * Lightweight InventoryOpenEvent analogue — no Bukkit event bus, no plugin tree walk.
 * Prefer {@link net.opmasterleo.packetuxui.PacketMenus#onInventoryOpen(GuiOpenListener)} when you
 * only care about opens (zero cost when none registered).
 */
@FunctionalInterface
public interface GuiOpenListener {

    void onInventoryOpen(GuiOpenEvent event);
}
