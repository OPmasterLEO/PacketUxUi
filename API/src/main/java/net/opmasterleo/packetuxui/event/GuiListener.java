package net.opmasterleo.packetuxui.event;

/**
 * Bukkit inventory events, but packet-native.
 * Register via {@link GuiEventManager#register(GuiListener)} or {@link net.opmasterleo.packetuxui.PacketMenus#registerListener}.
 */
public interface GuiListener {

    default GuiListenerPriority priority() {
        return GuiListenerPriority.NORMAL;
    }

    /** {@code InventoryOpenEvent} analogue — or use {@link net.opmasterleo.packetuxui.PacketMenus#onInventoryOpen}. */
    default void onOpen(GuiOpenEvent event) {
    }

    /** {@code InventoryCloseEvent} analogue. */
    default void onClose(GuiCloseEvent event) {
    }

    /** {@code InventoryClickEvent} analogue — cancel to block handling. */
    default void onClick(GuiClickEvent event) {
    }

    default void onClickPost(GuiClickPostEvent event) {
    }

    /** {@code InventoryDragEvent} analogue (START/ADD/END). Cancel to reject. */
    default void onDrag(GuiDragEvent event) {
    }
}
