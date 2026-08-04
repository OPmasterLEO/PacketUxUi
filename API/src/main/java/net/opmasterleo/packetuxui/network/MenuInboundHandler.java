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
                boolean hadVirtualMenu = menuService.hasOpen(player.getUniqueId());
                int closeId = classifier.closeWindowId(msg);
                boolean oursClose = menuService.isOursWindow(player.getUniqueId(), closeId);
                if (menuService.debugLogging()) {
                    menuService.debug(player, "CLOSE closeId=" + closeId
                            + " hadVirtual=" + hadVirtualMenu
                            + " transition=" + menuService.isTransitionActive(player));
                }
                scheduler.runForPlayer(player, () -> menuService.onCloseMenu(player));
                // Never leak PacketUxUi window closes into vanilla / anticheat.
                if (hadVirtualMenu || oursClose || menuService.isTransitionActive(player)) {
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
                    if (menuService.isOursWindow(player.getUniqueId(), windowId)
                            || menuService.hasOpen(player.getUniqueId())) {
                        return;
                    }
                    ctx.fireChannelRead(msg);
                    return;
                }
                ClickPacket click = classifier.readClick(msg);
                if (click == null) {
                    if (menuService.isOursWindow(player.getUniqueId(), windowId)) {
                        return;
                    }
                    ctx.fireChannelRead(msg);
                    return;
                }
                // Netty: cursor-only clear (no stateId). Player thread does one SetContent.
                try {
                    menuService.correctReadOnlyClick(player, click);
                } catch (Throwable error) {
                    menuService.debug(player, "correctReadOnlyClick failed: " + error.getClass().getSimpleName()
                            + " slot=" + click.slot() + " stateId=" + click.stateId());
                }
                scheduler.runForPlayer(player, () -> menuService.handleIncomingClick(player, click));
            }
            case OTHER -> ctx.fireChannelRead(msg);
        }
    }
}
