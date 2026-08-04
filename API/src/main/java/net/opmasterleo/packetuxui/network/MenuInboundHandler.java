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
                    scheduler.runForPlayer(player, new CloseMenuTask(player, menuService));
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
                    scheduler.runForPlayer(player, new ForceResyncTask(player, menuService));
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
                scheduler.runForPlayer(player, new IncomingClickTask(player, menuService, click));
            }
            case OTHER -> ctx.fireChannelRead(msg);
        }
    }

    private static final class CloseMenuTask implements Runnable {
        private final Player player;
        private final MenuService menuService;

        private CloseMenuTask(Player player, MenuService menuService) {
            this.player = player;
            this.menuService = menuService;
        }

        @Override
        public void run() {
            menuService.onCloseMenu(player);
        }
    }

    private static final class ForceResyncTask implements Runnable {
        private final Player player;
        private final MenuService menuService;

        private ForceResyncTask(Player player, MenuService menuService) {
            this.player = player;
            this.menuService = menuService;
        }

        @Override
        public void run() {
            menuService.forceResyncOpen(player);
        }
    }

    private static final class IncomingClickTask implements Runnable {
        private final Player player;
        private final MenuService menuService;
        private final ClickPacket click;

        private IncomingClickTask(Player player, MenuService menuService, ClickPacket click) {
            this.player = player;
            this.menuService = menuService;
            this.click = click;
        }

        @Override
        public void run() {
            menuService.handleIncomingClick(player, click);
        }
    }
}
