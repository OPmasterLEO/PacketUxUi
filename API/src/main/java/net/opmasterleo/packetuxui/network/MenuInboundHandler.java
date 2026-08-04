package net.opmasterleo.packetuxui.network;

import org.bukkit.entity.Player;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.NmsAdapter;
import net.opmasterleo.packetuxui.nms.PacketClassifier;
import net.opmasterleo.packetuxui.nms.WindowClickType;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.service.MenuService;

import java.util.Map;

/**
 * Intercepts container click/close for open PacketUxUi sessions. Must sit before
 * {@code packet_handler} so vanilla never mutates the bound menu / player inventory.
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
                if (hadVirtualMenu || oursClose || menuService.isTransitionActive(player)) {
                    return;
                }
                ctx.fireChannelRead(msg);
            }
            case CLICK -> {
                int windowId = classifier.clickWindowId(msg);
                boolean sessionOpen = menuService.hasOpen(player.getUniqueId());
                // While a virtual menu is open, NEVER leak clicks to vanilla (free client moves).
                if (sessionOpen) {
                    if (windowId < 0 || menuService.shouldIgnore(windowId, player)) {
                        if (menuService.debugLogging()) {
                            menuService.debug(player, "CLICK swallow mismatch windowId=" + windowId
                                    + " expected=" + menuService.getWindowId(player));
                        }
                        scheduler.runForPlayer(player, () -> menuService.forceResyncOpen(player));
                        return;
                    }
                    ClickPacket click = classifier.readClick(msg);
                    if (click == null) {
                        click = new ClickPacket(
                                windowId,
                                0,
                                -999,
                                0,
                                0,
                                WindowClickType.PICKUP,
                                Map.of(),
                                UxItem.EMPTY
                        );
                    }
                    try {
                        menuService.correctReadOnlyClick(player, click);
                    } catch (Throwable error) {
                        menuService.debug(player, "correctReadOnlyClick failed: "
                                + error.getClass().getSimpleName()
                                + " slot=" + click.slot() + " stateId=" + click.stateId());
                    }
                    ClickPacket finalClick = click;
                    scheduler.runForPlayer(player, () -> menuService.handleIncomingClick(player, finalClick));
                    return;
                }
                if (windowId < 0) {
                    ctx.fireChannelRead(msg);
                    return;
                }
                if (menuService.isOursWindow(player.getUniqueId(), windowId)) {
                    // Stale ours id without session — drop.
                    return;
                }
                ctx.fireChannelRead(msg);
            }
            case OTHER -> ctx.fireChannelRead(msg);
        }
    }
}
