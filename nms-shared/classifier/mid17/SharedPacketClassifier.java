package net.opmasterleo.packetuxui.nms.shared;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.ClickType;
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
    public int clickWindowId(Object packet) {
        if (packet instanceof ServerboundContainerClickPacket click) {
            return click.getContainerId();
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
        Int2ObjectMap<ItemStack> slots = click.f();
        Set<Integer> ids;
        if (slots == null || slots.isEmpty()) {
            ids = Set.of();
        } else {
            ids = new HashSet<>(slots.size());
            for (Int2ObjectMap.Entry<ItemStack> entry : slots.int2ObjectEntrySet()) {
                ids.add(entry.getIntKey());
            }
        }
        ItemStack carriedNms = click.getCarriedItem();
        boolean carriedEmpty = carriedNms == null || carriedNms.isEmpty();
        return ClickPacket.lazy(
                click.getContainerId(),
                click.getStateId(),
                click.getSlotNum(),
                click.getButtonNum(),
                0,
                fromNms(click.getClickType()),
                ids,
                carriedEmpty,
                new ChangedSlotsSupplier(slots),
                new CarriedSupplier(carriedEmpty, carriedNms)
        );
    }

    private Map<Integer, UxItem> decodeChanged(Int2ObjectMap<ItemStack> slots) {
        if (slots == null || slots.isEmpty()) {
            return Map.of();
        }
        Map<Integer, UxItem> changed = new HashMap<>(slots.size());
        for (Int2ObjectMap.Entry<ItemStack> entry : slots.int2ObjectEntrySet()) {
            changed.put(entry.getIntKey(), items.fromMinecraft(entry.getValue()));
        }
        return changed;
    }

    @Override
    public boolean isClose(Object packet) {
        return packet instanceof ServerboundContainerClosePacket;
    }

    private static WindowClickType fromNms(ClickType type) {
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

    private final class ChangedSlotsSupplier implements Supplier<Map<Integer, UxItem>> {
        private final Int2ObjectMap<ItemStack> slots;

        ChangedSlotsSupplier(Int2ObjectMap<ItemStack> slots) {
            this.slots = slots;
        }

        @Override
        public Map<Integer, UxItem> get() {
            return decodeChanged(slots);
        }
    }

    private final class CarriedSupplier implements Supplier<UxItem> {
        private final boolean carriedEmpty;
        private final ItemStack carriedNms;

        CarriedSupplier(boolean carriedEmpty, ItemStack carriedNms) {
            this.carriedEmpty = carriedEmpty;
            this.carriedNms = carriedNms;
        }

        @Override
        public UxItem get() {
            return carriedEmpty ? UxItem.EMPTY : items.fromMinecraft(carriedNms);
        }
    }
}
