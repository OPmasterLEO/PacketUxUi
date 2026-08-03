package net.opmasterleo.packetuxui.scheduler.bukkit;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import net.opmasterleo.packetuxui.scheduler.AsyncTasks;
import net.opmasterleo.packetuxui.scheduler.TaskHandle;

public final class BukkitAsyncTasks implements AsyncTasks {

    private final JavaPlugin plugin;

    public BukkitAsyncTasks(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void run(Runnable task) {
        Objects.requireNonNull(task, "task");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public TaskHandle runLater(Runnable task, long delay, TimeUnit unit) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(unit, "unit");
        long ticks = Math.max(1L, unit.toMillis(Math.max(1L, delay)) / 50L);
        return BukkitTaskHandles.of(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, ticks));
    }

    @Override
    public TaskHandle runRepeating(Runnable task, long initialDelay, long period, TimeUnit unit) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(unit, "unit");
        long delayTicks = Math.max(1L, unit.toMillis(Math.max(1L, initialDelay)) / 50L);
        long periodTicks = Math.max(1L, unit.toMillis(Math.max(1L, period)) / 50L);
        return BukkitTaskHandles.of(
                Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks),
                true
        );
    }
}
