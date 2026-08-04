package net.opmasterleo.packetuxui;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.controller.LifecycleListeners;
import net.opmasterleo.packetuxui.manager.PacketGuiManager;
import net.opmasterleo.packetuxui.network.PipelineManager;
import net.opmasterleo.packetuxui.nms.AdapterLoader;
import net.opmasterleo.packetuxui.nms.NmsAdapter;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.service.MenuBuilder;
import net.opmasterleo.packetuxui.service.MenuService;

public final class PacketUxUiAPI {

    public static final String VERSION = "0.12.7";

    private static final AtomicInteger RETAIN = new AtomicInteger();
    private static final Set<JavaPlugin> CLIENTS = new LinkedHashSet<>();

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

    public static net.opmasterleo.packetuxui.event.GuiEventManager getEventManager() {
        checkInitialized();
        return service.events();
    }

    public static java.util.List<String> getPipelineHandlers(Player player) {
        checkInitialized();
        if (player == null || pipelineManager == null) {
            return java.util.List.of();
        }
        return pipelineManager.pipelineHandlers(player);
    }

    public static Optional<String> nmsBucket() {
        return initialized && adapter != null
                ? Optional.of(adapter.bucketId())
                : Optional.empty();
    }

    public static boolean isAvailable() {
        return initialized
                || Bukkit.getServicesManager().getRegistration(PacketUxUiHolder.class) != null;
    }

    public static synchronized void init(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        if (initialized) {
            retain(plugin);
            return;
        }
        init(plugin, AdapterLoader.load());
    }

    public static synchronized void init(JavaPlugin plugin, NmsAdapter nmsAdapter) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(nmsAdapter, "nmsAdapter");
        if (initialized) {
            retain(plugin);
            return;
        }
        adapter = nmsAdapter;
        host = plugin;
        scheduler = new PlatformScheduler(plugin);
        service = new MenuService(adapter, scheduler);
        pipelineManager = new PipelineManager(plugin, adapter, service, scheduler);
        service.setPipelineReassert(pipelineManager::ensureInjected);
        if (Boolean.getBoolean("packetuxui.debug")
                || "true".equalsIgnoreCase(System.getenv("PACKETUXUI_DEBUG"))) {
            service.setDebugLogging(true);
        }
        net.opmasterleo.packetuxui.nms.map.BukkitKeyMaps.warmupDefaults();
        LifecycleListeners.register(plugin, service, pipelineManager, scheduler);
        for (Player player : Bukkit.getOnlinePlayers()) {
            pipelineManager.inject(player);
        }
        Bukkit.getServicesManager().register(
                PacketUxUiHolder.class,
                new PacketUxUiHolder(),
                plugin,
                ServicePriority.Normal
        );
        CLIENTS.clear();
        CLIENTS.add(plugin);
        RETAIN.set(1);
        initialized = true;
        plugin.getLogger().info(
                "PacketUxUi " + VERSION + " ready (NMS " + adapter.bucketId()
                        + ", protocol " + adapter.minProtocol() + ".." + adapter.maxProtocol()
                        + ", scheduler " + scheduler.kind()
                        + (scheduler.isFolia() ? "/folia" : "")
                        + ", debug=" + service.debugLogging()
                        + ")"
        );
        if (service.debugLogging()) {
            plugin.getLogger().info("PacketUxUi debug logging is ON (-Dpacketuxui.debug=true or PACKETUXUI_DEBUG)");
        }
    }

    public static synchronized void terminate(JavaPlugin plugin) {
        if (!initialized || plugin == null) {
            return;
        }
        if (!CLIENTS.remove(plugin)) {
            return;
        }
        if (RETAIN.decrementAndGet() > 0) {
            if (plugin == host) {
                transferHost();
            }
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

    public static void closeThen(Player player, Runnable onSettled) {
        getService().closeThen(player, onSettled);
    }

    public static void closeThen(Player player, long settleTicks, Runnable onSettled) {
        getService().closeThen(player, settleTicks, onSettled);
    }

    public static MenuBuilder menu(Component title, net.opmasterleo.packetuxui.types.InventoryType type) {
        return MenuBuilder.of(title, type);
    }

    public static MenuBuilder menu(String miniMessageTitle, net.opmasterleo.packetuxui.types.InventoryType type) {
        return MenuBuilder.of(miniMessageTitle, type);
    }

    private static void retain(JavaPlugin plugin) {
        if (CLIENTS.add(plugin)) {
            RETAIN.incrementAndGet();
        }
    }

    private static void transferHost() {
        JavaPlugin previous = host;
        JavaPlugin next = CLIENTS.iterator().next();
        host = next;
        try {
            if (previous != null) {
                Bukkit.getServicesManager().unregisterAll(previous);
            }
        } catch (Throwable ignored) {
        }
        Bukkit.getServicesManager().register(
                PacketUxUiHolder.class,
                new PacketUxUiHolder(),
                next,
                ServicePriority.Normal
        );
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
        if (service != null) {
            service.events().clear();
        }
        initialized = false;
        RETAIN.set(0);
        CLIENTS.clear();
        host = null;
        service = null;
        adapter = null;
        scheduler = null;
        pipelineManager = null;
        PacketGuiManager.resetHolder();
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
