package net.opmasterleo.packetuxui.nms.shared;

import java.util.List;

import org.bukkit.craftbukkit.NMS.entity.CraftPlayer;
import org.bukkit.craftbukkit.NMS.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.server.NMS.EntityPlayer;
import net.minecraft.server.NMS.IChatBaseComponent;
import net.minecraft.server.NMS.InventoryClickType;
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
import net.opmasterleo.packetuxui.nms.WindowClickType;
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
                legacyType(typeId),
                chat,
                slotCount(typeId)
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
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        ItemStack nmsItem = CraftItemStack.asNMSCopy(items.toBukkit(item));
        handle.playerConnection.sendPacket(new PacketPlayOutSetSlot(windowId, slot, nmsItem));
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

    private static InventoryClickType toInventoryClickType(WindowClickType type) {
        return switch (type) {
            case QUICK_MOVE -> InventoryClickType.QUICK_MOVE;
            case SWAP -> InventoryClickType.SWAP;
            case CLONE -> InventoryClickType.CLONE;
            case THROW -> InventoryClickType.THROW;
            case QUICK_CRAFT -> InventoryClickType.QUICK_CRAFT;
            case PICKUP_ALL -> InventoryClickType.PICKUP_ALL;
            default -> InventoryClickType.PICKUP;
        };
    }

    private static String legacyType(int typeId) {
        return switch (typeId) {
            case 6 -> "minecraft:dispenser";
            case 8 -> "minecraft:anvil";
            case 12 -> "minecraft:crafting_table";
            case 14 -> "minecraft:furnace";
            case 16 -> "minecraft:hopper";
            default -> "minecraft:chest";
        };
    }

    private static int slotCount(int typeId) {
        return switch (typeId) {
            case 0 -> 9;
            case 1 -> 18;
            case 2 -> 27;
            case 3 -> 36;
            case 4 -> 45;
            case 5 -> 54;
            case 6 -> 9;
            case 8 -> 3;
            case 12 -> 10;
            case 14 -> 3;
            case 16 -> 5;
            default -> 27;
        };
    }
}
