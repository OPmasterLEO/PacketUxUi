package net.opmasterleo.packetuxui.nms.shared;

import java.util.List;

import org.bukkit.craftbukkit.NMS.entity.CraftPlayer;
import org.bukkit.craftbukkit.NMS.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.server.NMS.Containers;
import net.minecraft.server.NMS.EntityPlayer;
import net.minecraft.server.NMS.IChatBaseComponent;
import net.minecraft.server.NMS.ItemStack;
import net.minecraft.server.NMS.NonNullList;
import net.minecraft.server.NMS.PacketDataSerializer;
import net.minecraft.server.NMS.PacketPlayInWindowClick;
import net.minecraft.server.NMS.PacketPlayOutCloseWindow;
import net.minecraft.server.NMS.PacketPlayOutOpenWindow;
import net.minecraft.server.NMS.PacketPlayOutSetSlot;
import net.minecraft.server.NMS.PacketPlayOutWindowItems;
import net.minecraft.server.NMS.PlayerConnection;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.MenuPacketBridge;
import net.opmasterleo.packetuxui.nms.item.UxItem;

public final class SharedMenuPacketBridge implements MenuPacketBridge {

    private final SharedItemBridge items;

    public SharedMenuPacketBridge(SharedItemBridge items) {
        this.items = items;
    }

    @Override
    public void sendOpenWindow(Player player, int windowId, int typeId, Component title) {
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        IChatBaseComponent chat = IChatBaseComponent.ChatSerializer.a(
                GsonComponentSerializer.gson().serialize(title)
        );
        handle.playerConnection.sendPacket(new PacketPlayOutOpenWindow(
                windowId,
                containerType(typeId),
                chat
        ));
    }

    @Override
    public void sendCloseWindow(Player player, int windowId) {
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        handle.playerConnection.sendPacket(new PacketPlayOutCloseWindow(windowId));
    }

    @Override
    public void sendWindowItems(Player player, int windowId, int stateId, List<UxItem> uxItems, UxItem carried) {
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        NonNullList<ItemStack> list = NonNullList.a(uxItems.size(), ItemStack.a);
        for (int i = 0; i < uxItems.size(); i++) {
            list.set(i, CraftItemStack.asNMSCopy(items.toBukkit(uxItems.get(i))));
        }
        handle.playerConnection.sendPacket(new PacketPlayOutWindowItems(windowId, list));
    }

    @Override
    public void sendSetSlot(Player player, int windowId, int stateId, int slot, UxItem item) {
        if (slot < 0) {
            sendCursorItem(player, item);
            return;
        }
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        ItemStack nmsItem = CraftItemStack.asNMSCopy(items.toBukkit(item));
        handle.playerConnection.sendPacket(new PacketPlayOutSetSlot(windowId, slot, nmsItem));
    }

    @Override
    public void sendCursorItem(Player player, UxItem item) {
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        ItemStack nmsItem = CraftItemStack.asNMSCopy(items.toBukkit(item));
        handle.playerConnection.sendPacket(new PacketPlayOutSetSlot(-1, -1, nmsItem));
    }
    @Override
    public void injectClick(Player player, ClickPacket click) {
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        PlayerConnection connection = handle.playerConnection;
        PacketDataSerializer buf = new PacketDataSerializer(Unpooled.buffer());
        buf.writeByte(click.windowId());
        buf.writeShort(click.slot());
        buf.writeByte(click.button());
        buf.writeShort(click.actionNumber());
        buf.a(toInventoryClickType(click.clickType()));
        buf.a(CraftItemStack.asNMSCopy(items.toBukkit(click.carried())));
        PacketPlayInWindowClick packet = new PacketPlayInWindowClick();
        try {
            packet.a(buf);
        } catch (Exception error) {
            throw new IllegalStateException("Failed to decode window click", error);
        }
        connection.a(packet);
    }

    private static net.minecraft.server.NMS.InventoryClickType toInventoryClickType(
            net.opmasterleo.packetuxui.nms.WindowClickType type
    ) {
        return switch (type) {
            case QUICK_MOVE -> net.minecraft.server.NMS.InventoryClickType.QUICK_MOVE;
            case SWAP -> net.minecraft.server.NMS.InventoryClickType.SWAP;
            case CLONE -> net.minecraft.server.NMS.InventoryClickType.CLONE;
            case THROW -> net.minecraft.server.NMS.InventoryClickType.THROW;
            case QUICK_CRAFT -> net.minecraft.server.NMS.InventoryClickType.QUICK_CRAFT;
            case PICKUP_ALL -> net.minecraft.server.NMS.InventoryClickType.PICKUP_ALL;
            default -> net.minecraft.server.NMS.InventoryClickType.PICKUP;
        };
    }

    @SuppressWarnings("rawtypes")
    private static Containers containerType(int typeId) {
        Containers[] types = {
                Containers.GENERIC_9X1,
                Containers.GENERIC_9X2,
                Containers.GENERIC_9X3,
                Containers.GENERIC_9X4,
                Containers.GENERIC_9X5,
                Containers.GENERIC_9X6,
                Containers.GENERIC_3X3,
                Containers.GENERIC_3X3,
                Containers.ANVIL,
                Containers.BEACON,
                Containers.BLAST_FURNACE,
                Containers.BREWING_STAND,
                Containers.CRAFTING,
                Containers.ENCHANTMENT,
                Containers.FURNACE,
                Containers.GRINDSTONE,
                Containers.HOPPER,
                Containers.LECTERN,
                Containers.LOOM,
                Containers.MERCHANT,
                Containers.SHULKER_BOX,
                Containers.GENERIC_3X3,
                Containers.SMOKER,
                Containers.CARTOGRAPHY_TABLE,
                Containers.STONECUTTER
        };
        if (typeId >= 0 && typeId < types.length) {
            return types[typeId];
        }
        return Containers.GENERIC_9X6;
    }
}
