package net.opmasterleo.packetuxui.scheduler.paper;

import java.util.Objects;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import net.opmasterleo.packetuxui.scheduler.EntityTasks;
import net.opmasterleo.packetuxui.scheduler.ServerPlatform;
import net.opmasterleo.packetuxui.scheduler.TaskHandle;

public final class PaperEntityTasks implements EntityTasks {

    private final JavaPlugin plugin;

    public PaperEntityTasks(JavaPlugin plugin) {
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
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            task.run();
            return;
        }
        entity.getScheduler().execute(plugin, task, retired, 0L);
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
        return PaperTaskHandles.of(
                entity.getScheduler().run(plugin, PaperConsumers.run(task), retired)
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
        if (delay <= 0L) {
            if (Bukkit.isOwnedByCurrentRegion(entity)) {
                task.run();
                return TaskHandle.NOOP;
            }
            // Zero delay off-region: next entity tick with a handle (Folia EntityScheduler.run).
            return PaperTaskHandles.of(
                    entity.getScheduler().run(plugin, PaperConsumers.run(task), retired)
            );
        }
        return PaperTaskHandles.of(
                entity.getScheduler().runDelayed(plugin, PaperConsumers.run(task), retired, delay)
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
        return PaperTaskHandles.of(entity.getScheduler().runAtFixedRate(
                plugin,
                PaperConsumers.accept(entity, task),
                retired,
                ServerPlatform.periodTicks(initialDelayTicks),
                ServerPlatform.periodTicks(periodTicks)
        ));
    }
}
