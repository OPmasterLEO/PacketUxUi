package net.opmasterleo.packetuxui.network;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.NmsAdapter;
import net.opmasterleo.packetuxui.nms.PacketClassifier;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.service.MenuService;

public final class PipelineManager {

    public static final String HANDLER_NAME = "packetuxui_inbound";

    private final NmsAdapter adapter;
    private final MenuService menuService;
    private final PlatformScheduler scheduler;
    private final ConcurrentHashMap<UUID, Boolean> injected = new ConcurrentHashMap<>();

    public PipelineManager(
            JavaPlugin plugin,
            NmsAdapter adapter,
            MenuService menuService,
            PlatformScheduler scheduler
    ) {
        this.adapter = adapter;
        this.menuService = menuService;
        this.scheduler = scheduler;
    }

    public void inject(Player player) {
        Channel channel = adapter.pipeline().channel(player);
        if (channel == null) {
            return;
        }
        channel.eventLoop().execute(() -> {
            try {
                if (channel.pipeline().get(HANDLER_NAME) != null) {
                    channel.pipeline().remove(HANDLER_NAME);
                }
                MenuInboundHandler handler = new MenuInboundHandler(player, adapter, menuService, scheduler);
                boolean added = false;
                for (String anchor : adapter.pipeline().injectBeforeNames()) {
                    if (channel.pipeline().get(anchor) != null) {
                        channel.pipeline().addBefore(anchor, HANDLER_NAME, handler);
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    channel.pipeline().addFirst(HANDLER_NAME, handler);
                }
                injected.put(player.getUniqueId(), Boolean.TRUE);
            } catch (Throwable ignored) {
            }
        });
    }

    public void remove(Player player) {
        injected.remove(player.getUniqueId());
        Channel channel = adapter.pipeline().channel(player);
        if (channel == null) {
            return;
        }
        channel.eventLoop().execute(() -> {
            try {
                if (channel.pipeline().get(HANDLER_NAME) != null) {
                    channel.pipeline().remove(HANDLER_NAME);
                }
            } catch (Throwable ignored) {
            }
        });
    }
}
