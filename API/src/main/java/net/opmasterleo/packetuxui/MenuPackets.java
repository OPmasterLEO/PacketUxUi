package net.opmasterleo.packetuxui;

import java.util.List;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.nms.MenuPacketBridge;
import net.opmasterleo.packetuxui.nms.item.UxItem;

public final class MenuPackets {

    private MenuPackets() {
    }

    public static MenuPacketBridge bridge() {
        return PacketUxUiAPI.getAdapter().packets();
    }

    public static int allocateWindowId(Player player) {
        return bridge().allocateWindowId(player);
    }

    /**
     * Bump bound NMS stateId past {@code clientFloor}, or {@code -1} if no bound menu
     * (caller should use session counter via {@link net.opmasterleo.packetuxui.service.MenuService}).
     */
    public static int nextStateId(Player player, int clientFloor) {
        return bridge().bumpStateId(player, clientFloor);
    }

    public static void open(Player player, int windowId, int typeId, Component title) {
        bridge().sendOpenWindow(player, windowId, typeId, title);
    }

    public static void close(Player player, int windowId) {
        bridge().sendCloseWindow(player, windowId);
    }

    public static void setContent(Player player, int windowId, int stateId, List<UxItem> items, UxItem carried) {
        bridge().sendWindowItems(player, windowId, stateId, items, carried);
    }

    public static void setSlot(Player player, int windowId, int stateId, int slot, UxItem item) {
        bridge().sendSetSlot(player, windowId, stateId, slot, item);
    }

    public static void setCursor(Player player, UxItem item) {
        bridge().sendCursorItem(player, item);
    }

    public static void bindChest(Player player, int windowId, int typeId, int rows) {
        bridge().bindServerContainer(player, windowId, typeId, rows);
    }

    public static void unbind(Player player) {
        bridge().unbindServerContainer(player);
    }

    public static boolean ownsBound(Player player) {
        return bridge().ownsBoundContainer(player);
    }
}
