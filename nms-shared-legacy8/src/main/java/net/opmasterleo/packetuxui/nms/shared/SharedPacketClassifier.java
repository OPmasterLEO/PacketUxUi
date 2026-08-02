package net.opmasterleo.packetuxui.nms.shared;

import java.util.Collections;

import net.minecraft.server.NMS.PacketPlayInCloseWindow;
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
        return Kind.OTHER;
    }

    @Override
    public ClickPacket readClick(Object packet) {
        if (!(packet instanceof PacketPlayInWindowClick click)) {
            return null;
        }
        return new ClickPacket(
                click.a(),
                0,
                click.b(),
                click.c(),
                click.d(),
                mapMode(click.f()),
                Collections.emptyMap(),
                UxItem.EMPTY
        );
    }

    @Override
    public boolean isClose(Object packet) {
        return packet instanceof PacketPlayInCloseWindow;
    }

    private static WindowClickType mapMode(int mode) {
        switch (mode) {
            case 0:
                return WindowClickType.PICKUP;
            case 1:
                return WindowClickType.QUICK_MOVE;
            case 2:
                return WindowClickType.SWAP;
            case 3:
                return WindowClickType.CLONE;
            case 4:
                return WindowClickType.THROW;
            case 5:
                return WindowClickType.QUICK_CRAFT;
            case 6:
                return WindowClickType.PICKUP_ALL;
            default:
                return WindowClickType.UNKNOWN;
        }
    }
}
