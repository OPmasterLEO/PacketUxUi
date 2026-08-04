package net.opmasterleo.packetuxui.event;

/**
 * Register via {@link GuiEventManager#register(GuiListener)} or {@link net.opmasterleo.packetuxui.PacketMenus#registerListener}.
 */
public interface GuiListener {

    default GuiListenerPriority priority() {
        return GuiListenerPriority.NORMAL;
    }

    default void onOpen(GuiOpenEvent event) {
    }

    default void onClose(GuiCloseEvent event) {
    }

    /** Pre-handle; may {@link GuiClickEvent#setCancelled(boolean)}. */
    default void onClick(GuiClickEvent event) {
    }

    default void onClickPost(GuiClickPostEvent event) {
    }
}
