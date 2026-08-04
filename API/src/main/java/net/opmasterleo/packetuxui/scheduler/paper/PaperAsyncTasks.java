package net.opmasterleo.packetuxui.scheduler.paper;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import net.opmasterleo.packetuxui.scheduler.AsyncTasks;
import net.opmasterleo.packetuxui.scheduler.TaskHandle;

public final class PaperAsyncTasks implements AsyncTasks {

    private final JavaPlugin plugin;

    public PaperAsyncTasks(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void run(Runnable task) {
        Objects.requireNonNull(task, "task");
        Bukkit.getAsyncScheduler().runNow(plugin, PaperConsumers.run(task));
    }

    @Override
    public TaskHandle runLater(Runnable task, long delay, TimeUnit unit) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(unit, "unit");
        return PaperTaskHandles.of(
                Bukkit.getAsyncScheduler().runDelayed(plugin, PaperConsumers.run(task), Math.max(1L, delay), unit)
        );
    }

    @Override
    public TaskHandle runRepeating(Runnable task, long initialDelay, long period, TimeUnit unit) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(unit, "unit");
        return PaperTaskHandles.of(Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin,
                PaperConsumers.run(task),
                Math.max(1L, initialDelay),
                Math.max(1L, period),
                unit
        ));
    }
}
