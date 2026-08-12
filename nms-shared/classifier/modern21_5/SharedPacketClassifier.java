package net.opmasterleo.packetuxui.nms.shared;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.inventory.ClickType;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.PacketClassifier;
import net.opmasterleo.packetuxui.nms.WindowClickType;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.nms.map.OrdinalMaps;

/** Direct NMS classification — {@code instanceof} only; click-type ordinal map. */
public final class SharedPacketClassifier implements PacketClassifier {

    private static final WindowClickType[] FROM_CLICK;
    private static final Supplier<Map<Integer, UxItem>> EMPTY_CHANGED = new EmptyChangedSlots();
    private static final Supplier<UxItem> EMPTY_CARRIED = new EmptyCarried();

    static {
        FROM_CLICK = new WindowClickType[ClickType.values().length];
        OrdinalMaps.fill(ClickType.values(), FROM_CLICK, new FromClickMapper());
    }

    private static final class FromClickMapper implements OrdinalMaps.EnumMapper<ClickType, WindowClickType> {
        @Override
        public WindowClickType map(ClickType type) {
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

    private static final class EmptyChangedSlots implements Supplier<Map<Integer, UxItem>> {
        @Override
        public Map<Integer, UxItem> get() {
            return Map.of();
        }
    }

    private static final class EmptyCarried implements Supplier<UxItem> {
        @Override
        public UxItem get() {
            return UxItem.EMPTY;
        }
    }

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
        if (packet instanceof ServerboundSignUpdatePacket) {
            return Kind.SIGN_UPDATE;
        }
        return Kind.OTHER;
    }

    @Override
    public int clickWindowId(Object packet) {
        if (packet instanceof ServerboundContainerClickPacket click) {
            return click.containerId();
        }
        return -1;
    }

    @Override
    public int closeWindowId(Object packet) {
        if (packet instanceof ServerboundContainerClosePacket close) {
            return close.getContainerId();
        }
        return -1;
    }

    @Override
    public ClickPacket readClick(Object packet) {
        if (!(packet instanceof ServerboundContainerClickPacket click)) {
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
                fromNms(click.clickType()),
                ids,
                carriedEmpty,
                EMPTY_CHANGED,
                EMPTY_CARRIED
        );
    }

    @Override
    public boolean isClose(Object packet) {
        return packet instanceof ServerboundContainerClosePacket;
    }

    private static boolean isHashedEmpty(HashedStack stack) {
        return stack == null || stack == HashedStack.EMPTY || stack.equals(HashedStack.EMPTY);
    }

    private static WindowClickType fromNms(ClickType type) {
        if (type == null) {
            return WindowClickType.UNKNOWN;
        }
        int i = type.ordinal();
        if (i >= 0 && i < FROM_CLICK.length && FROM_CLICK[i] != null) {
            return FROM_CLICK[i];
        }
        return WindowClickType.UNKNOWN;
    }
}
