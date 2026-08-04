package net.opmasterleo.packetuxui.nms.shared;

import org.bukkit.craftbukkit.NMS.entity.CraftPlayer;
import org.bukkit.entity.Player;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
            for (Connection connection : connections()) {
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

    @SuppressWarnings("unchecked")
    private static Iterable<Connection> connections() throws Exception {
        Object listener = MinecraftServer.getServer().getConnection();
        var field = listener.getClass().getDeclaredField("connections");
        field.setAccessible(true);
        return (Iterable<Connection>) field.get(listener);
    }

    private static Connection resolveConnection(ServerPlayer sp) {
        Object listener = sp.connection;
        if (listener == null) {
            return null;
        }
        try {
            Class<?> type = listener.getClass();
            while (type != null && type != Object.class) {
                try {
                    var field = type.getDeclaredField("connection");
                    field.setAccessible(true);
                    Object value = field.get(listener);
                    if (value instanceof Connection network) {
                        return network;
                    }
                } catch (NoSuchFieldException ignored) {
                }
                type = type.getSuperclass();
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }
}
