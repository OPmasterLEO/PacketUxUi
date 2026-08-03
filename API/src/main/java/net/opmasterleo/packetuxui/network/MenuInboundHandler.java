package net.opmasterleo.packetuxui.network;

import org.bukkit.entity.Player;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.NmsAdapter;
import net.opmasterleo.packetuxui.nms.PacketClassifier;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.service.MenuService;

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
                scheduler.runForPlayer(player, () -> menuService.onCloseMenu(player));
                ctx.fireChannelRead(msg);
            }
            case CLICK -> {
                int windowId = classifier.clickWindowId(msg);
                if (windowId < 0 || menuService.shouldIgnore(windowId, player)) {
                    ctx.fireChannelRead(msg);
                    return;
                }
                ClickPacket click = classifier.readClick(msg);
                if (click == null) {
                    ctx.fireChannelRead(msg);
                    return;
                }
                scheduler.runForPlayer(player, () -> menuService.handleIncomingClick(player, click));
            }
            case OTHER -> ctx.fireChannelRead(msg);
        }
    }
}
