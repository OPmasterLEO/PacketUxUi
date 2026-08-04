package net.opmasterleo.packetuxui.scheduler.bukkit;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
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
            Bukkit.getScheduler().runTask(plugin, wrap(entity, task, retired));
        }
    }

    @Override
    public TaskHandle runNextTick(Entity entity, Runnable task, Runnable retired) {
        Objects.requireNonNull(task, "task");
        if (entity == null || !entity.isValid()) {
            if (retired != null) {
                retired.run();
            }
            return TaskHandle.NOOP;
        }
        return BukkitTaskHandles.of(
                Bukkit.getScheduler().runTask(plugin, wrap(entity, task, retired))
        );
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
        long delay = ServerPlatform.delayTicks(delayTicks);
        if (delay <= 0L && Bukkit.isPrimaryThread()) {
            if (!entity.isValid()) {
                if (retired != null) {
                    retired.run();
                }
                return TaskHandle.NOOP;
            }
            task.run();
            return TaskHandle.NOOP;
        }
        return BukkitTaskHandles.of(
                Bukkit.getScheduler().runTaskLater(
                        plugin,
                        wrap(entity, task, retired),
                        delay <= 0L ? 1L : delay
                )
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
        AtomicReference<org.bukkit.scheduler.BukkitTask> taskRef = new AtomicReference<>();
        org.bukkit.scheduler.BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!entity.isValid()) {
                org.bukkit.scheduler.BukkitTask self = taskRef.get();
                if (self != null) {
                    self.cancel();
                }
                if (retired != null) {
                    retired.run();
                }
                return;
            }
            task.accept(entity);
        }, ServerPlatform.periodTicks(initialDelayTicks), ServerPlatform.periodTicks(periodTicks));
        taskRef.set(bukkitTask);
        return BukkitTaskHandles.of(bukkitTask, true);
    }

    private static Runnable wrap(Entity entity, Runnable task, Runnable retired) {
        return () -> {
            if (entity == null || !entity.isValid()) {
                if (retired != null) {
                    retired.run();
                }
                return;
            }
            task.run();
        };
    }
}
