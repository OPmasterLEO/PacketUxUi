package net.opmasterleo.packetuxui.network;

import org.bukkit.entity.Player;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.NmsAdapter;
import net.opmasterleo.packetuxui.nms.PacketClassifier;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.service.MenuService;
import net.opmasterleo.packetuxui.service.WindowIdPool;

public final class MenuInboundHandler extends ChannelInboundHandlerAdapter {

    private final Player player;
    private final NmsAdapter adapter;
    private final MenuService menuService;
    private final PlatformScheduler scheduler;

    public MenuInboundHandler(
            Player player,
            NmsAdapter adapter,
            MenuService menuService,
            PlatformScheduler scheduler
    ) {
        this.player = player;
        this.adapter = adapter;
        this.menuService = menuService;
        this.scheduler = scheduler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        PacketClassifier classifier = adapter.classifier();
        PacketClassifier.Kind kind = classifier.kindOf(msg);
        switch (kind) {
            case CLOSE -> {
                boolean hadVirtualMenu = menuService.hasOpen(player.getUniqueId());
                int closeId = classifier.closeWindowId(msg);
                scheduler.runForPlayer(player, () -> menuService.onCloseMenu(player));
                if (hadVirtualMenu || WindowIdPool.isVirtual(closeId)) {
                    return;
                }
                ctx.fireChannelRead(msg);
            }
            case CLICK -> {
                int windowId = classifier.clickWindowId(msg);
                if (windowId < 0) {
                    ctx.fireChannelRead(msg);
                    return;
                }
                if (menuService.shouldIgnore(windowId, player)) {
                    if (WindowIdPool.isVirtual(windowId) || menuService.hasOpen(player.getUniqueId())) {
                        return;
                    }
                    ctx.fireChannelRead(msg);
                    return;
                }
                ClickPacket click = classifier.readClick(msg);
                if (click == null) {
                    if (WindowIdPool.isVirtual(windowId)) {
                        return;
                    }
                    ctx.fireChannelRead(msg);
                    return;
                }
                // Correct optimistic client pickup on the netty thread BEFORE the scheduled
                // click handler runs — otherwise Lunar/vanilla prediction keeps the button
                // on the cursor for one or more frames (or forever if stateId is stale).
                try {
                    menuService.correctReadOnlyClick(player, click);
                } catch (Throwable ignored) {
                }
                scheduler.runForPlayer(player, () -> menuService.handleIncomingClick(player, click));
            }
            case OTHER -> ctx.fireChannelRead(msg);
        }
    }
}
