package net.opmasterleo.packetuxui.nms;

import org.bukkit.entity.Player;

import io.netty.channel.Channel;

public interface PipelineBridge {

    Channel channel(Player player);

    String[] injectBeforeNames();
}
