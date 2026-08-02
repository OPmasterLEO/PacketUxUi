package net.opmasterleo.packetuxui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.opmasterleo.packetuxui.controller.BukkitListener;
import net.opmasterleo.packetuxui.network.PipelineManager;
import net.opmasterleo.packetuxui.nms.NmsAdapter;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.service.MenuService;

public final class PacketUxUiAPI {

    private static boolean initialized;
    private static MenuService service;
    private static NmsAdapter adapter;
    private static PlatformScheduler scheduler;
    private static PipelineManager pipelineManager;

    private PacketUxUiAPI() {
    }

    public static boolean isInitialized() {
        return initialized;
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

    public static void init(JavaPlugin plugin, NmsAdapter nmsAdapter) {
        if (initialized) {
            return;
        }
        adapter = nmsAdapter;
        scheduler = new PlatformScheduler(plugin);
        service = new MenuService(adapter, scheduler);
        pipelineManager = new PipelineManager(plugin, adapter, service, scheduler);
        Bukkit.getPluginManager().registerEvents(new BukkitListener(service, pipelineManager, scheduler), plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            pipelineManager.inject(player);
        }
        initialized = true;
    }

    public static void terminate() {
        if (!initialized) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            pipelineManager.remove(player);
        }
        initialized = false;
    }

    private static void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("PacketUxUiAPI is not initialized. Call init() first.");
        }
    }
}
