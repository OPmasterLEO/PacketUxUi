package net.opmasterleo.packetuxui.nms.shared;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.opmasterleo.packetuxui.nms.PipelineBridge;

public final class SharedPipelineBridge implements PipelineBridge {

    private static final String[] ANCHORS = {
            "packet_handler",
            "inbound_config",
            "outbound_config",
            "encoder",
            "prepender",
            "compress",
            "decrypt"
    };

    @Override
    public Channel channel(Player player) {
        if (!(player instanceof CraftPlayer craft)) {
            return null;
        }
        ServerPlayer sp = craft.getHandle();
        Connection connection = resolveConnection(sp);
        if (connection != null && connection.channel != null) {
            return connection.channel;
        }
        return channelByAddress(player);
    }

    @Override
    public String[] injectBeforeNames() {
        return ANCHORS.clone();
    }

    private static Channel channelByAddress(Player player) {
        if (player == null || player.getAddress() == null) {
            return null;
        }
        try {
            var address = player.getAddress().getAddress();
            for (Connection connection : MinecraftServer.getServer().getConnection().getConnections()) {
                if (connection == null || connection.channel == null) {
                    continue;
                }
                if (connection.getRemoteAddress() instanceof java.net.InetSocketAddress remote
                        && address.equals(remote.getAddress())) {
                    return connection.channel;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Connection resolveConnection(ServerPlayer sp) {
        if (sp.connection instanceof ServerGamePacketListenerImpl listener) {
            return listener.connection;
        }
        return null;
    }
}
