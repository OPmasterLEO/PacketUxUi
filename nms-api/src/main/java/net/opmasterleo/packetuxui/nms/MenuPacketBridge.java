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

    default void bindServerContainer(Player player, int windowId, int typeId, int rows) {
    }

    default void unbindServerContainer(Player player) {
    }
}
