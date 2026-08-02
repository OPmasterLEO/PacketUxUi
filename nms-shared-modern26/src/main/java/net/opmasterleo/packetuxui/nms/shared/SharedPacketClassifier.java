package net.opmasterleo.packetuxui.nms.shared;

import java.util.HashMap;
import java.util.Map;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
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
        if (packet instanceof ServerboundContainerClickPacket) {
            return Kind.CLICK;
        }
        if (packet instanceof ServerboundContainerClosePacket) {
            return Kind.CLOSE;
        }
        return Kind.OTHER;
    }

    @Override
    public ClickPacket readClick(Object packet) {
        if (!(packet instanceof ServerboundContainerClickPacket click)) {
            return null;
        }
        Map<Integer, UxItem> changed = new HashMap<>();
        return new ClickPacket(
                click.containerId(),
                click.stateId(),
                click.slotNum(),
                click.buttonNum(),
                0,
                fromNms(click.containerInput()),
                changed,
                UxItem.EMPTY
        );
    }

    @Override
    public boolean isClose(Object packet) {
        return packet instanceof ServerboundContainerClosePacket;
    }

    private static WindowClickType fromNms(ContainerInput type) {
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
