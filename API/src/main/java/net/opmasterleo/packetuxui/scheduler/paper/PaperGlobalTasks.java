package net.opmasterleo.packetuxui.scheduler.paper;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import net.opmasterleo.packetuxui.scheduler.GlobalTasks;
import net.opmasterleo.packetuxui.scheduler.ServerPlatform;
import net.opmasterleo.packetuxui.scheduler.TaskHandle;

public final class PaperGlobalTasks implements GlobalTasks {

    private final JavaPlugin plugin;

    public PaperGlobalTasks(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void run(Runnable task) {
        Objects.requireNonNull(task, "task");
        if (Bukkit.getServer().isGlobalTickThread()) {
            task.run();
            return;
        }
        Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    }

    @Override
    public TaskHandle runNextTick(Runnable task) {
        Objects.requireNonNull(task, "task");
        return PaperTaskHandles.of(Bukkit.getGlobalRegionScheduler().run(plugin, st -> task.run()));
    }

    @Override
    public TaskHandle runLater(Runnable task, long delayTicks) {
        Objects.requireNonNull(task, "task");
        return PaperTaskHandles.of(
                Bukkit.getGlobalRegionScheduler().runDelayed(plugin, st -> task.run(), ServerPlatform.ticks(delayTicks))
        );
    }

    @Override
    public TaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(task, "task");
        return PaperTaskHandles.of(Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                st -> task.run(),
                ServerPlatform.ticks(initialDelayTicks),
                ServerPlatform.ticks(periodTicks)
        ));
    }
}
