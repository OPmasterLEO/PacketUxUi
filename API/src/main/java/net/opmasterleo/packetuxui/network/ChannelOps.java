package net.opmasterleo.packetuxui.network;

import java.util.List;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;

final class ChannelOps {

    private ChannelOps() {
    }

    static void runInEventLoop(Channel channel, Runnable runnable) {
        if (channel == null || runnable == null) {
            return;
        }
        if (channel.eventLoop().inEventLoop()) {
            runnable.run();
            return;
        }
        channel.eventLoop().execute(runnable);
    }

    static List<String> pipelineNames(Channel channel) {
        if (channel == null) {
            return List.of();
        }
        try {
            return List.copyOf(channel.pipeline().names());
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    static ChannelHandler get(Channel channel, String name) {
        if (channel == null || name == null) {
            return null;
        }
        try {
            return channel.pipeline().get(name);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
