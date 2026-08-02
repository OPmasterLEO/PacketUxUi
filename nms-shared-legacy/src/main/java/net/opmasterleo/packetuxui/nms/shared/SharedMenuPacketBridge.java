package net.opmasterleo.packetuxui.nms.shared;

import java.util.Arrays;
import java.util.List;

import org.bukkit.craftbukkit.NMS.entity.CraftPlayer;
import org.bukkit.craftbukkit.NMS.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.server.NMS.EntityPlayer;
import net.minecraft.server.NMS.IChatBaseComponent;
import net.minecraft.server.NMS.ItemStack;
import net.minecraft.server.NMS.PacketPlayInWindowClick;
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
                legacyType(typeId),
                chat,
                slotCount(typeId)
        ));
    }

    @Override
    public void sendWindowItems(Player player, int windowId, int stateId, List<UxItem> uxItems, UxItem carried) {
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        ItemStack[] nmsItems = new ItemStack[uxItems.size()];
        for (int i = 0; i < uxItems.size(); i++) {
            nmsItems[i] = CraftItemStack.asNMSCopy(items.toBukkit(uxItems.get(i)));
        }
        handle.playerConnection.sendPacket(new PacketPlayOutWindowItems(windowId, Arrays.asList(nmsItems)));
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
        PacketPlayInWindowClick packet = new PacketPlayInWindowClick(
                click.windowId(),
                click.slot(),
                click.button(),
                click.clickType().ordinal(),
                CraftItemStack.asNMSCopy(items.toBukkit(click.carried())),
                (short) click.actionNumber()
        );
        connection.a(packet);
    }

    private static String legacyType(int typeId) {
        switch (typeId) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return "minecraft:chest";
            case 6:
                return "minecraft:dispenser";
            case 8:
                return "minecraft:anvil";
            case 12:
                return "minecraft:crafting_table";
            case 14:
                return "minecraft:furnace";
            case 16:
                return "minecraft:hopper";
            default:
                return "minecraft:chest";
        }
    }

    private static int slotCount(int typeId) {
        switch (typeId) {
            case 0:
                return 9;
            case 1:
                return 18;
            case 2:
                return 27;
            case 3:
                return 36;
            case 4:
                return 45;
            case 5:
                return 54;
            case 6:
                return 9;
            case 8:
                return 3;
            case 12:
                return 10;
            case 14:
                return 3;
            case 16:
                return 5;
            default:
                return 27;
        }
    }
}
