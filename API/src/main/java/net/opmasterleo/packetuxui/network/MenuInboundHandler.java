package net.opmasterleo.packetuxui.network;

import java.util.Map;

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

/**
 * Server-authoritative intercept: while a PacketUxUi session is open, container
 * click/close never reach vanilla ({@code packet_handler}). Also detects PacketEvents
 * wrappers by class name when {@code instanceof} fails.
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
            ctx.fireChannelRead(msg);
            return;
        }
        PacketClassifier classifier = adapter.classifier();
        PacketClassifier.Kind kind = classifier.kindOf(msg);
        if (kind == PacketClassifier.Kind.OTHER) {
            kind = heuristicKind(msg);
        }
        boolean sessionOpen = menuService.hasOpen(player.getUniqueId());

        switch (kind) {
            case CLOSE -> {
                int closeId = classifier.closeWindowId(msg);
                if (closeId < 0) {
                    closeId = reflectiveInt(msg, "containerId", "getContainerId", "windowId", "getWindowId");
                }
                boolean oursClose = menuService.isOursWindow(player.getUniqueId(), closeId);
                if (menuService.debugLogging()) {
                    menuService.debug(player, "CLOSE closeId=" + closeId
                            + " hadVirtual=" + sessionOpen
                            + " class=" + msg.getClass().getName());
                }
                if (sessionOpen || oursClose || menuService.isTransitionActive(player)) {
                    scheduler.runForPlayer(player, () -> menuService.onCloseMenu(player));
                    return; // never leak to vanilla
                }
                ctx.fireChannelRead(msg);
            }
            case CLICK -> {
                if (!sessionOpen) {
                    int windowId = classifier.clickWindowId(msg);
                    if (windowId < 0) {
                        windowId = reflectiveInt(msg, "containerId", "getContainerId", "windowId");
                    }
                    if (menuService.isOursWindow(player.getUniqueId(), windowId)) {
                        return;
                    }
                    ctx.fireChannelRead(msg);
                    return;
                }
                // Session open → SERVER owns GUI. Drop vanilla path entirely.
                int windowId = classifier.clickWindowId(msg);
                if (windowId < 0) {
                    windowId = reflectiveInt(msg, "containerId", "getContainerId", "windowId");
                }
                ClickPacket click = classifier.readClick(msg);
                if (click == null) {
                    int stateId = reflectiveInt(msg, "stateId", "getStateId");
                    int slot = reflectiveInt(msg, "slotNum", "getSlotNum", "slot", "getSlot");
                    int button = reflectiveInt(msg, "buttonNum", "getButtonNum", "buttonId", "getButton");
                    if (stateId == Integer.MIN_VALUE) {
                        stateId = 0;
                    }
                    if (slot == Integer.MIN_VALUE) {
                        slot = -999;
                    }
                    if (button == Integer.MIN_VALUE) {
                        button = 0;
                    }
                    click = new ClickPacket(
                            windowId >= 0 ? windowId : menuService.getWindowId(player),
                            Math.max(0, stateId),
                            slot,
                            button,
                            0,
                            WindowClickType.PICKUP,
                            Map.of(),
                            UxItem.EMPTY
                    );
                }
                try {
                    // Immediate server→client authority (stops Lunar free-move).
                    menuService.suppressClientPrediction(player, click);
                } catch (Throwable error) {
                    menuService.debug(player, "suppressClientPrediction failed: "
                            + error.getClass().getSimpleName());
                }
                ClickPacket finalClick = click;
                scheduler.runForPlayer(player, () -> menuService.handleIncomingClick(player, finalClick));
                return;
            }
            case OTHER -> {
                // While open, drop unknown container-ish packets that PE/Via may disguise.
                if (sessionOpen && looksLikeContainerPacket(msg)) {
                    if (menuService.debugLogging()) {
                        menuService.debug(player, "DROP other-container class=" + msg.getClass().getName());
                    }
                    scheduler.runForPlayer(player, () -> menuService.forceResyncOpen(player));
                    return;
                }
                ctx.fireChannelRead(msg);
            }
        }
    }

    private static PacketClassifier.Kind heuristicKind(Object msg) {
        String n = msg.getClass().getName();
        String simple = msg.getClass().getSimpleName();
        if (containsAny(n, simple,
                "ContainerClick", "ClickWindow", "WindowClick", "ServerboundContainerClick")) {
            return PacketClassifier.Kind.CLICK;
        }
        if (containsAny(n, simple,
                "ContainerClose", "CloseWindow", "ServerboundContainerClose")) {
            return PacketClassifier.Kind.CLOSE;
        }
        return PacketClassifier.Kind.OTHER;
    }

    private static boolean looksLikeContainerPacket(Object msg) {
        String n = msg.getClass().getName().toLowerCase();
        return n.contains("container") || n.contains("inventory") || n.contains("window");
    }

    private static boolean containsAny(String full, String simple, String... needles) {
        for (String needle : needles) {
            if (full.contains(needle) || simple.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static int reflectiveInt(Object msg, String... names) {
        Class<?> c = msg.getClass();
        for (String name : names) {
            try {
                var method = c.getMethod(name);
                Object v = method.invoke(msg);
                if (v instanceof Number number) {
                    return number.intValue();
                }
            } catch (Throwable ignored) {
            }
            try {
                var field = c.getDeclaredField(name);
                field.setAccessible(true);
                Object v = field.get(msg);
                if (v instanceof Number number) {
                    return number.intValue();
                }
            } catch (Throwable ignored) {
            }
        }
        // PacketEvents wrappers often expose getNativePacket / getPacket
        for (String getter : new String[]{"getNativePacket", "getPacket", "getWrapper"}) {
            try {
                var method = c.getMethod(getter);
                Object inner = method.invoke(msg);
                if (inner != null && inner != msg) {
                    int nested = reflectiveInt(inner, names);
                    if (nested != Integer.MIN_VALUE) {
                        return nested;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return Integer.MIN_VALUE;
    }
}
