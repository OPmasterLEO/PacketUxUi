package net.opmasterleo.packetuxui.nms.shared;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.NMS.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.server.NMS.BlockPosition;
import net.minecraft.server.NMS.ChatComponentText;
import net.minecraft.server.NMS.EntityPlayer;
import net.minecraft.server.NMS.EnumColor;
import net.minecraft.server.NMS.PacketPlayInUpdateSign;
import net.minecraft.server.NMS.PacketPlayOutOpenSignEditor;
import net.minecraft.server.NMS.TileEntitySign;
import net.opmasterleo.packetuxui.nms.SignOpenRequest;
import net.opmasterleo.packetuxui.nms.SignPacketBridge;
import net.opmasterleo.packetuxui.nms.SignUpdate;

public final class SharedSignPackets implements SignPacketBridge {

    @Override
    public boolean open(Player player, SignOpenRequest request) {
        return send(player, request, true);
    }

    @Override
    public boolean refresh(Player player, SignOpenRequest request) {
        return send(player, request, false);
    }

    private boolean send(Player player, SignOpenRequest request, boolean placeBlock) {
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        Location loc = new Location(player.getWorld(), request.x(), request.y(), request.z());
        BlockPosition pos = new BlockPosition(request.x(), request.y(), request.z());
        TileEntitySign sign = new TileEntitySign();
        sign.setPosition(pos);
        applyColor(sign, request.dyeColor());
        String[] lines = request.legacyLines();
        for (int i = 0; i < 4; i++) {
            sign.a(i, new ChatComponentText(lines[i]));
        }
        if (placeBlock) {
            player.sendBlockChange(loc, material(request.materialName()).createBlockData());
        }
        handle.playerConnection.sendPacket(sign.getUpdatePacket());
        handle.playerConnection.sendPacket(new PacketPlayOutOpenSignEditor(pos));
        return true;
    }

    @Override
    public void close(Player player, int x, int y, int z) {
        Location loc = new Location(player.getWorld(), x, y, z);
        player.sendBlockChange(loc, loc.getBlock().getBlockData());
    }

    @Override
    public SignUpdate readUpdate(Object packet) {
        if (!(packet instanceof PacketPlayInUpdateSign update)) {
            return null;
        }
        BlockPosition pos = update.b();
        return new SignUpdate(pos.getX(), pos.getY(), pos.getZ(), update.c(), true);
    }

    private static void applyColor(TileEntitySign sign, String colorName) {
        EnumColor color;
        try {
            color = EnumColor.valueOf(colorName);
        } catch (IllegalArgumentException ignored) {
            color = EnumColor.BLACK;
        }
        try {
            Method setColor = TileEntitySign.class.getMethod("setColor", EnumColor.class);
            setColor.invoke(sign, color);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        for (String fieldName : new String[] {"color", "l"}) {
            try {
                Field field = TileEntitySign.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(sign, color);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static Material material(String name) {
        Material type = name == null ? null : Material.getMaterial(name);
        if (type != null) {
            return type;
        }
        type = Material.getMaterial("OAK_WALL_SIGN");
        if (type != null) {
            return type;
        }
        type = Material.getMaterial("WALL_SIGN");
        return type != null ? type : Material.AIR;
    }
}
