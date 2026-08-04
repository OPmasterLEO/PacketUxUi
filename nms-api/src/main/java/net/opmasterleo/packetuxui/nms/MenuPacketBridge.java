package net.opmasterleo.packetuxui.nms;

import java.util.List;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.nms.item.UxItem;

public interface MenuPacketBridge {

    void sendOpenWindow(Player player, int windowId, int typeId, Component title);

    void sendCloseWindow(Player player, int windowId);

    void sendWindowItems(Player player, int windowId, int stateId, List<UxItem> items, UxItem carried);

    void sendSetSlot(Player player, int windowId, int stateId, int slot, UxItem item);

    void sendCursorItem(Player player, UxItem item);

    void injectClick(Player player, ClickPacket click);

    /**
     * Allocate the next vanilla container sync id (typically {@code ServerPlayer.nextContainerCounter()},
     * cycling 1..100). Fallback adapters use {@link FallbackWindowIds}.
     */
    default int allocateWindowId(Player player) {
        return FallbackWindowIds.next(player.getUniqueId());
    }

    /**
     * Advance the bound NMS menu's stateId past {@code clientFloor}.
     *
     * @return the new stateId, or {@code -1} if this bridge does not own NMS state (caller uses session)
     */
    default int bumpStateId(Player player, int clientFloor) {
        return -1;
    }

    /**
     * Write top-slot stacks into the bound server container so AC/size inspection matches packets.
     */
    default void mirrorTopSlots(Player player, List<UxItem> topItems) {
    }

    default void mirrorSlot(Player player, int slot, UxItem item) {
    }

    /** True when {@code player.containerMenu} is a PacketUxUi-bound menu. */
    default boolean ownsBoundContainer(Player player) {
        return false;
    }

    /**
     * Bind an inert {@code ChestMenu} for generic 9xN types only. Non-chest typeIds must no-op
     * (do not lie to anticheat with the wrong menu size).
     */
    default void bindServerContainer(Player player, int windowId, int typeId, int rows) {
    }

    default void unbindServerContainer(Player player) {
    }
}
