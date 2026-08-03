package net.opmasterleo.packetuxui.scheduler.bukkit;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import net.opmasterleo.packetuxui.scheduler.GlobalTasks;
import net.opmasterleo.packetuxui.scheduler.ServerPlatform;
import net.opmasterleo.packetuxui.scheduler.TaskHandle;

public final class BukkitGlobalTasks implements GlobalTasks {

    private final JavaPlugin plugin;

    public BukkitGlobalTasks(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void run(Runnable task) {
        Objects.requireNonNull(task, "task");
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    @Override
    public TaskHandle runNextTick(Runnable task) {
        Objects.requireNonNull(task, "task");
        return BukkitTaskHandles.of(Bukkit.getScheduler().runTask(plugin, task));
    }

    @Override
    public TaskHandle runLater(Runnable task, long delayTicks) {
        Objects.requireNonNull(task, "task");
        return BukkitTaskHandles.of(
                Bukkit.getScheduler().runTaskLater(plugin, task, ServerPlatform.ticks(delayTicks))
        );
    }

    @Override
    public TaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(task, "task");
        return BukkitTaskHandles.of(
                Bukkit.getScheduler().runTaskTimer(
                        plugin,
                        task,
                        ServerPlatform.ticks(initialDelayTicks),
                        ServerPlatform.ticks(periodTicks)
                ),
                true
        );
    }
}
