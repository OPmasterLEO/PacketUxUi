package net.opmasterleo.packetuxui.nms.shared;

import java.util.Collections;

import net.minecraft.server.NMS.InventoryClickType;
import net.minecraft.server.NMS.PacketPlayInCloseWindow;
import net.minecraft.server.NMS.PacketPlayInUpdateSign;
import net.minecraft.server.NMS.PacketPlayInWindowClick;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.PacketClassifier;
import net.opmasterleo.packetuxui.nms.WindowClickType;
import net.opmasterleo.packetuxui.nms.item.UxItem;

public final class SharedPacketClassifier implements PacketClassifier {

    private final SharedItemBridge items;

    public SharedPacketClassifier(SharedItemBridge items) {
        this.items = items;
    }

    @Override
    public Kind kindOf(Object packet) {
        if (packet instanceof PacketPlayInWindowClick) {
            return Kind.CLICK;
        }
        if (packet instanceof PacketPlayInCloseWindow) {
            return Kind.CLOSE;
        }
        if (packet instanceof PacketPlayInUpdateSign) {
            return Kind.SIGN_UPDATE;
        }
        return Kind.OTHER;
    }

    @Override
    public int clickWindowId(Object packet) {
        if (packet instanceof PacketPlayInWindowClick click) {
            return click.b();
        }
        return -1;
    }

    @Override
    public ClickPacket readClick(Object packet) {
        if (!(packet instanceof PacketPlayInWindowClick click)) {
            return null;
        }
        return new ClickPacket(
                click.b(),
                0,
                click.c(),
                click.d(),
                click.e(),
                mapMode(click.g()),
                Collections.emptyMap(),
                UxItem.EMPTY
        );
    }

    @Override
    public boolean isClose(Object packet) {
        return packet instanceof PacketPlayInCloseWindow;
    }

    private static WindowClickType mapMode(InventoryClickType type) {
        if (type == null) {
            return WindowClickType.UNKNOWN;
        }
        return switch (type) {
            case PICKUP -> WindowClickType.PICKUP;
            case QUICK_MOVE -> WindowClickType.QUICK_MOVE;
            case SWAP -> WindowClickType.SWAP;
            case CLONE -> WindowClickType.CLONE;
            case THROW -> WindowClickType.THROW;
            case QUICK_CRAFT -> WindowClickType.QUICK_CRAFT;
            case PICKUP_ALL -> WindowClickType.PICKUP_ALL;
            default -> WindowClickType.UNKNOWN;
        };
    }
}
