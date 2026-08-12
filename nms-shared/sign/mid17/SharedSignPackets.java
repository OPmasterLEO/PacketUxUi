package net.opmasterleo.packetuxui.nms.shared;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.NMS.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignBlockEntity;
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
        ServerPlayer sp = nms(player);
        if (sp == null) {
            return false;
        }
        Location loc = new Location(player.getWorld(), request.x(), request.y(), request.z());
        BlockPos pos = new BlockPos(request.x(), request.y(), request.z());
        SignBlockEntity sign = new SignBlockEntity(pos, null);
        sign.setColor(dye(request.dyeColor()));
        sign.setHasGlowingText(request.glow());
        Component[] lines = request.lines();
        for (int i = 0; i < 4; i++) {
            sign.setMessage(i, toNms(lines[i]));
        }
        if (placeBlock) {
            player.sendBlockChange(loc, material(request.materialName()).createBlockData());
        }
        sp.connection.send(sign.getUpdatePacket());
        sp.connection.send(new ClientboundOpenSignEditorPacket(pos));
        return true;
    }

    @Override
    public void close(Player player, int x, int y, int z) {
        Location loc = new Location(player.getWorld(), x, y, z);
        player.sendBlockChange(loc, loc.getBlock().getBlockData());
    }

    @Override
    public SignUpdate readUpdate(Object packet) {
        if (!(packet instanceof ServerboundSignUpdatePacket update)) {
            return null;
        }
        BlockPos pos = update.getPos();
        return new SignUpdate(pos.getX(), pos.getY(), pos.getZ(), update.getLines(), true);
    }

    private static net.minecraft.network.chat.Component toNms(Component line) {
        net.minecraft.network.chat.Component nms = net.minecraft.network.chat.Component.Serializer.fromJson(
                GsonComponentSerializer.gson().serialize(line == null ? Component.empty() : line)
        );
        return nms == null ? net.minecraft.network.chat.Component.nullToEmpty("") : nms;
    }

    private static DyeColor dye(String name) {
        try {
            return DyeColor.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return DyeColor.BLACK;
        }
    }

    private static Material material(String name) {
        Material type = name == null ? null : Material.getMaterial(name);
        if (type != null) {
            return type;
        }
        type = Material.getMaterial("OAK_WALL_SIGN");
        return type != null ? type : Material.AIR;
    }

    private static ServerPlayer nms(Player player) {
        if (player instanceof CraftPlayer craft) {
            return craft.getHandle();
        }
        return null;
    }
}
