package net.opmasterleo.packetuxui.network;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import net.opmasterleo.packetuxui.nms.NmsAdapter;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.service.MenuService;

/**
 * Injects {@link MenuInboundHandler} <b>before</b> {@code packet_handler} so virtual-menu
 * clicks never reach vanilla. Prefer after Via/PacketEvents decoder when present.
 */
public final class PipelineManager {

    public static final String HANDLER_NAME_PREFIX = "packetuxui_inbound_";

    /** Decoded-play anchors: inject after these when present. */
    private static final String[] INJECT_AFTER = {
            "packetevents_decoder",
            "pe-decoder",
            "via-decoder",
            "via_decoder",
            "decoder"
    };

    /** Must stay before these so ServerGamePacketListenerImpl never sees our clicks. */
    private static final String[] INJECT_BEFORE = {
            "packet_handler",
            "inbound_config",
            "outbound_config"
    };

    private final NmsAdapter adapter;
    private final MenuService menuService;
    private final PlatformScheduler scheduler;
    private final String handlerName;
    private final ConcurrentMap<UUID, Boolean> injected = new ConcurrentHashMap<>();

    public PipelineManager(
            JavaPlugin plugin,
            NmsAdapter adapter,
            MenuService menuService,
            PlatformScheduler scheduler
    ) {
        this.adapter = adapter;
        this.menuService = menuService;
        this.scheduler = scheduler;
        this.handlerName = HANDLER_NAME_PREFIX + sanitize(plugin.getName());
    }

    public void inject(Player player) {
        if (player == null) {
            return;
        }
        Channel channel = adapter.pipeline().channel(player);
        if (channel == null) {
            injected.remove(player.getUniqueId());
            return;
        }
        ChannelOps.runInEventLoop(channel, () -> install(player, channel, false));
    }

    /** Re-inject if missing or channel replaced (call on every present/open). */
    public void ensureInjected(Player player) {
        if (player == null) {
            return;
        }
        Channel channel = adapter.pipeline().channel(player);
        if (channel == null) {
            injected.remove(player.getUniqueId());
            return;
        }
        ChannelOps.runInEventLoop(channel, () -> {
            ChannelHandler existing = ChannelOps.get(channel, handlerName);
            if (existing instanceof MenuInboundHandler) {
                injected.put(player.getUniqueId(), Boolean.TRUE);
                return;
            }
            install(player, channel, true);
        });
    }

    public boolean isInjected(Player player) {
        if (player == null) {
            return false;
        }
        Channel channel = adapter.pipeline().channel(player);
        if (channel == null) {
            return false;
        }
        return ChannelOps.get(channel, handlerName) instanceof MenuInboundHandler;
    }

    public void remove(Player player) {
        if (player == null) {
            return;
        }
        injected.remove(player.getUniqueId());
        Channel channel = adapter.pipeline().channel(player);
        if (channel == null) {
            return;
        }
        ChannelOps.runInEventLoop(channel, () -> {
            try {
                if (ChannelOps.get(channel, handlerName) != null) {
                    channel.pipeline().remove(handlerName);
                }
            } catch (Throwable error) {
                if (menuService.debugLogging()) {
                    menuService.debug(player, "pipeline remove failed: " + error.getClass().getSimpleName());
                }
            }
        });
    }

    public List<String> pipelineHandlers(Player player) {
        Channel channel = adapter.pipeline().channel(player);
        return ChannelOps.pipelineNames(channel);
    }

    public String handlerName() {
        return handlerName;
    }

    private void install(Player player, Channel channel, boolean reassert) {
        try {
            if (ChannelOps.get(channel, handlerName) != null) {
                channel.pipeline().remove(handlerName);
            }
            MenuInboundHandler handler = new MenuInboundHandler(player, adapter, menuService, scheduler);
            boolean added = false;
            // Prefer immediately before packet_handler so vanilla never sees clicks
            // (server-authoritative GUIs; PE wrappers still intercepted by class-name).
            for (String anchor : INJECT_BEFORE) {
                if (ChannelOps.get(channel, anchor) != null) {
                    channel.pipeline().addBefore(anchor, handlerName, handler);
                    added = true;
                    break;
                }
            }
            if (!added) {
                for (String anchor : adapter.pipeline().injectBeforeNames()) {
                    if (ChannelOps.get(channel, anchor) != null) {
                        channel.pipeline().addBefore(anchor, handlerName, handler);
                        added = true;
                        break;
                    }
                }
            }
            if (!added) {
                for (String anchor : INJECT_AFTER) {
                    if (ChannelOps.get(channel, anchor) != null) {
                        channel.pipeline().addAfter(anchor, handlerName, handler);
                        added = true;
                        break;
                    }
                }
            }
            // Never addFirst — undecoded bytes → kind OTHER → leak to vanilla → free client moves.
            if (!added) {
                injected.remove(player.getUniqueId());
                if (menuService.debugLogging()) {
                    menuService.debug(player, "pipeline inject FAILED no safe anchor names="
                            + ChannelOps.pipelineNames(channel));
                }
                return;
            }
            injected.put(player.getUniqueId(), Boolean.TRUE);
            if (menuService.debugLogging()) {
                menuService.debug(player, (reassert ? "pipeline reassert OK" : "pipeline inject OK")
                        + " before=" + neighborBefore(channel)
                        + " after=" + neighborAfter(channel));
            }
        } catch (Throwable error) {
            injected.remove(player.getUniqueId());
            if (menuService.debugLogging()) {
                menuService.debug(player, "pipeline inject failed: " + error.getClass().getSimpleName()
                        + " names=" + ChannelOps.pipelineNames(channel));
            }
        }
    }

    private String neighborBefore(Channel channel) {
        try {
            List<String> names = ChannelOps.pipelineNames(channel);
            int idx = names.indexOf(handlerName);
            return idx > 0 ? names.get(idx - 1) : "?";
        } catch (Throwable ignored) {
            return "?";
        }
    }

    private String neighborAfter(Channel channel) {
        try {
            List<String> names = ChannelOps.pipelineNames(channel);
            int idx = names.indexOf(handlerName);
            return idx >= 0 && idx + 1 < names.size() ? names.get(idx + 1) : "?";
        } catch (Throwable ignored) {
            return "?";
        }
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "default";
        }
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
    }
}
