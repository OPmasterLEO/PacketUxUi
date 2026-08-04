package net.opmasterleo.packetuxui.nms.shared;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.ContainerInput;
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
        Object unwrapped = unwrap(packet);
        if (unwrapped != null && unwrapped != packet) {
            return kindOf(unwrapped);
        }
        String name = packet.getClass().getName();
        if (name.contains("ContainerClick") || name.contains("ClickWindow") || name.contains("WindowClick")) {
            return Kind.CLICK;
        }
        if (name.contains("ContainerClose") || name.contains("CloseWindow")) {
            return Kind.CLOSE;
        }
        return Kind.OTHER;
    }

    @Override
    public int clickWindowId(Object packet) {
        if (packet instanceof ServerboundContainerClickPacket click) {
            return click.containerId();
        }
        Object unwrapped = unwrap(packet);
        if (unwrapped instanceof ServerboundContainerClickPacket click) {
            return click.containerId();
        }
        return -1;
    }

    @Override
    public int closeWindowId(Object packet) {
        if (packet instanceof ServerboundContainerClosePacket close) {
            return close.getContainerId();
        }
        Object unwrapped = unwrap(packet);
        if (unwrapped instanceof ServerboundContainerClosePacket close) {
            return close.getContainerId();
        }
        return -1;
    }

    @Override
    public ClickPacket readClick(Object packet) {
        ServerboundContainerClickPacket click = null;
        if (packet instanceof ServerboundContainerClickPacket direct) {
            click = direct;
        } else {
            Object unwrapped = unwrap(packet);
            if (unwrapped instanceof ServerboundContainerClickPacket nested) {
                click = nested;
            }
        }
        if (click == null) {
            return null;
        }
        Int2ObjectMap<HashedStack> slots = click.changedSlots();
        Set<Integer> ids;
        if (slots == null || slots.isEmpty()) {
            ids = Set.of();
        } else {
            ids = new HashSet<>(slots.size());
            for (Int2ObjectMap.Entry<HashedStack> entry : slots.int2ObjectEntrySet()) {
                ids.add(entry.getIntKey());
            }
        }
        HashedStack carried = click.carriedItem();
        boolean carriedEmpty = isHashedEmpty(carried);
        return ClickPacket.lazy(
                click.containerId(),
                click.stateId(),
                click.slotNum(),
                click.buttonNum(),
                0,
                fromNms(click.containerInput()),
                ids,
                carriedEmpty,
                () -> Map.of(),
                () -> UxItem.EMPTY
        );
    }

    @Override
    public boolean isClose(Object packet) {
        return kindOf(packet) == Kind.CLOSE;
    }

    private static Object unwrap(Object packet) {
        if (packet == null) {
            return null;
        }
        Class<?> c = packet.getClass();
        for (String getter : new String[]{"getNativePacket", "getPacket", "getWrapper", "getHandle"}) {
            try {
                var method = c.getMethod(getter);
                Object inner = method.invoke(packet);
                if (inner != null && inner != packet) {
                    return inner;
                }
            } catch (Throwable ignored) {
            }
        }
        return packet;
    }

    private static boolean isHashedEmpty(HashedStack stack) {
        return stack == null || stack == HashedStack.EMPTY || stack.equals(HashedStack.EMPTY);
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
