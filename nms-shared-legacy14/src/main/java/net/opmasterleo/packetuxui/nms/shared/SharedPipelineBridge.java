package net.opmasterleo.packetuxui.nms.shared;

import org.bukkit.craftbukkit.NMS.entity.CraftPlayer;
import org.bukkit.entity.Player;

import io.netty.channel.Channel;
import net.minecraft.server.NMS.EntityPlayer;
import net.minecraft.server.NMS.NetworkManager;
import net.minecraft.server.NMS.PlayerConnection;
import net.opmasterleo.packetuxui.nms.PipelineBridge;

public final class SharedPipelineBridge implements PipelineBridge {

    private static final String[] ANCHORS = {
            "packet_handler", "encoder", "prepender", "compress", "decrypt"
    };

    @Override
    public Channel channel(Player player) {
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        PlayerConnection connection = handle.playerConnection;
        NetworkManager network = connection.networkManager;
        return network.channel;
    }

    @Override
    public String[] injectBeforeNames() {
        return ANCHORS.clone();
    }
}
