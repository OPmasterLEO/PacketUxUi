package net.opmasterleo.packetuxui.network;

import org.bukkit.entity.Player;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.NmsAdapter;
import net.opmasterleo.packetuxui.nms.PacketClassifier;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.service.MenuService;

/**
 * Server-authoritative intercept: while a PacketUxUi session is open, container
 * click/close never reach vanilla {@code packet_handler}. Classification is
 * direct NMS via {@link PacketClassifier} (per-bucket adapters) — no reflection.
 */
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
        if (msg == null) {
            ctx.fireChannelRead(null);
            return;
        }
        PacketClassifier classifier = adapter.classifier();
        PacketClassifier.Kind kind = classifier.kindOf(msg);
        boolean sessionOpen = menuService.hasOpen(player.getUniqueId());

        switch (kind) {
            case CLOSE -> {
                int closeId = classifier.closeWindowId(msg);
                boolean oursClose = menuService.isOursWindow(player.getUniqueId(), closeId);
                if (menuService.debugLogging()) {
                    menuService.debug(player, "CLOSE closeId=" + closeId
                            + " hadVirtual=" + sessionOpen);
                }
                if (sessionOpen || oursClose || menuService.isTransitionActive(player)) {
                    scheduler.runForPlayer(player, () -> menuService.onCloseMenu(player));
                    return;
                }
                ctx.fireChannelRead(msg);
            }
            case CLICK -> {
                if (!sessionOpen) {
                    int windowId = classifier.clickWindowId(msg);
                    if (menuService.isOursWindow(player.getUniqueId(), windowId)) {
                        return;
                    }
                    ctx.fireChannelRead(msg);
                    return;
                }
                ClickPacket click = classifier.readClick(msg);
                if (click == null) {
                    // Unknown shape for this bucket — still drop so vanilla cannot mutate.
                    scheduler.runForPlayer(player, () -> menuService.forceResyncOpen(player));
                    return;
                }
                try {
                    menuService.suppressClientPrediction(player, click);
                } catch (Throwable error) {
                    if (menuService.debugLogging()) {
                        menuService.debug(player, "suppressClientPrediction failed: "
                                + error.getClass().getSimpleName());
                    }
                }
                ClickPacket finalClick = click;
                scheduler.runForPlayer(player, () -> menuService.handleIncomingClick(player, finalClick));
            }
            case OTHER -> ctx.fireChannelRead(msg);
        }
    }
}
