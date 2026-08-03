package net.opmasterleo.packetuxui.scheduler.bukkit;

import java.util.Objects;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import net.opmasterleo.packetuxui.scheduler.EntityTasks;
import net.opmasterleo.packetuxui.scheduler.ServerPlatform;
import net.opmasterleo.packetuxui.scheduler.TaskHandle;

public final class BukkitEntityTasks implements EntityTasks {

    private final JavaPlugin plugin;

    public BukkitEntityTasks(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void run(Entity entity, Runnable task, Runnable retired) {
        Objects.requireNonNull(task, "task");
        if (entity == null || !entity.isValid()) {
            if (retired != null) {
                retired.run();
            }
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    @Override
    public TaskHandle runLater(Entity entity, Runnable task, Runnable retired, long delayTicks) {
        Objects.requireNonNull(task, "task");
        if (entity == null || !entity.isValid()) {
            if (retired != null) {
                retired.run();
            }
            return TaskHandle.NOOP;
        }
        return BukkitTaskHandles.of(
                Bukkit.getScheduler().runTaskLater(plugin, task, ServerPlatform.ticks(delayTicks))
        );
    }

    @Override
    public TaskHandle runRepeating(
            Entity entity,
            Consumer<? super Entity> task,
            Runnable retired,
            long initialDelayTicks,
            long periodTicks
    ) {
        Objects.requireNonNull(task, "task");
        if (entity == null || !entity.isValid()) {
            if (retired != null) {
                retired.run();
            }
            return TaskHandle.NOOP;
        }
        return BukkitTaskHandles.of(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (entity.isValid()) {
                task.accept(entity);
            }
        }, ServerPlatform.ticks(initialDelayTicks), ServerPlatform.ticks(periodTicks)), true);
    }
}
