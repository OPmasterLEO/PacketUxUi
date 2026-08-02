package net.opmasterleo.packetuxui;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.controller.BukkitListener;
import net.opmasterleo.packetuxui.network.PipelineManager;
import net.opmasterleo.packetuxui.nms.AdapterLoader;
import net.opmasterleo.packetuxui.nms.NmsAdapter;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.service.MenuBuilder;
import net.opmasterleo.packetuxui.service.MenuService;

public final class PacketUxUiAPI {

    public static final String VERSION = "1.0.0";

    private static final AtomicInteger RETAIN = new AtomicInteger();

    private static volatile boolean initialized;
    private static volatile JavaPlugin host;
    private static MenuService service;
    private static NmsAdapter adapter;
    private static PlatformScheduler scheduler;
    private static PipelineManager pipelineManager;

    private PacketUxUiAPI() {
    }

    public static String version() {
        return VERSION;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static JavaPlugin host() {
        return host;
    }

    public static MenuService getService() {
        checkInitialized();
        return service;
    }

    public static NmsAdapter getAdapter() {
        checkInitialized();
        return adapter;
    }

    public static PlatformScheduler getScheduler() {
        checkInitialized();
        return scheduler;
    }

    public static Optional<String> nmsBucket() {
        return initialized && adapter != null
                ? Optional.of(adapter.bucketId())
                : Optional.empty();
    }

    /**
     * Soft-depend check when another plugin hosts PacketUxUi via
     * {@link org.bukkit.plugin.ServicesManager}.
     */
    public static boolean isAvailable() {
        return initialized
                || Bukkit.getServicesManager().getRegistration(PacketUxUiHolder.class) != null;
    }

    public static synchronized void init(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        if (initialized) {
            RETAIN.incrementAndGet();
            return;
        }
        init(plugin, AdapterLoader.load());
    }

    public static synchronized void init(JavaPlugin plugin, NmsAdapter nmsAdapter) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(nmsAdapter, "nmsAdapter");
        if (initialized) {
            RETAIN.incrementAndGet();
            return;
        }
        adapter = nmsAdapter;
        host = plugin;
        scheduler = new PlatformScheduler(plugin);
        service = new MenuService(adapter, scheduler);
        pipelineManager = new PipelineManager(plugin, adapter, service, scheduler);
        Bukkit.getPluginManager().registerEvents(new BukkitListener(service, pipelineManager, scheduler), plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            pipelineManager.inject(player);
        }
        Bukkit.getServicesManager().register(
                PacketUxUiHolder.class,
                new PacketUxUiHolder(),
                plugin,
                ServicePriority.Normal
        );
        RETAIN.set(1);
        initialized = true;
        plugin.getLogger().info(
                "PacketUxUi " + VERSION + " ready (NMS " + adapter.bucketId()
                        + ", protocol " + adapter.minProtocol() + ".." + adapter.maxProtocol() + ")"
        );
    }

    public static synchronized void terminate(JavaPlugin plugin) {
        if (!initialized) {
            return;
        }
        if (plugin != null && host != null && plugin != host && RETAIN.get() > 1) {
            RETAIN.decrementAndGet();
            return;
        }
        if (RETAIN.decrementAndGet() > 0 && plugin != host) {
            return;
        }
        shutdown();
    }

    public static synchronized void terminate() {
        if (!initialized) {
            return;
        }
        shutdown();
    }

    public static void open(Player player, Menu menu) {
        getService().openMenu(player, menu);
    }

    public static void close(Player player) {
        getService().closeMenu(player);
    }

    public static MenuBuilder menu(Component title, net.opmasterleo.packetuxui.types.InventoryType type) {
        return MenuBuilder.of(title, type);
    }

    public static MenuBuilder menu(String miniMessageTitle, net.opmasterleo.packetuxui.types.InventoryType type) {
        return MenuBuilder.of(miniMessageTitle, type);
    }

    private static void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                service.closeMenu(player);
            } catch (Throwable ignored) {
            }
            try {
                pipelineManager.remove(player);
            } catch (Throwable ignored) {
            }
        }
        if (host != null) {
            Bukkit.getServicesManager().unregisterAll(host);
        }
        initialized = false;
        RETAIN.set(0);
        host = null;
        service = null;
        adapter = null;
        scheduler = null;
        pipelineManager = null;
    }

    private static void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                    "PacketUxUiAPI is not initialized. Call PacketUxUiAPI.init(plugin) in onEnable."
            );
        }
    }

    public static final class PacketUxUiHolder {
    }
}
