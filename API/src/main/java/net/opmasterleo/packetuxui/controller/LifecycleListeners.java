package net.opmasterleo.packetuxui.controller;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import net.opmasterleo.packetuxui.network.PipelineManager;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.scheduler.SchedulerKind;
import net.opmasterleo.packetuxui.service.MenuService;

public final class LifecycleListeners {

    private LifecycleListeners() {
    }

    public static Listener register(
            JavaPlugin plugin,
            MenuService service,
            PipelineManager pipelineManager,
            PlatformScheduler scheduler
    ) {
        Listener listener = create(service, pipelineManager, scheduler);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        return listener;
    }

    public static Listener create(
            MenuService service,
            PipelineManager pipelineManager,
            PlatformScheduler scheduler
    ) {
        if (scheduler.kind() == SchedulerKind.PAPER) {
            return new PaperLifecycleListener(service, pipelineManager, scheduler);
        }
        return new BukkitLifecycleListener(service, pipelineManager, scheduler);
    }
}
