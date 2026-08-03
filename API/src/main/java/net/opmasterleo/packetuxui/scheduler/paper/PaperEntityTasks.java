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
    public TaskHandle runLater(Entity entity, Runnable task, Runnable retired, long delayTicks) {
        Objects.requireNonNull(task, "task");
        if (entity == null || !entity.isValid()) {
            if (retired != null) {
                retired.run();
            }
            return TaskHandle.NOOP;
        }
        return PaperTaskHandles.of(
                entity.getScheduler().runDelayed(plugin, st -> task.run(), retired, ServerPlatform.ticks(delayTicks))
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
                st -> task.accept(entity),
                retired,
                ServerPlatform.ticks(initialDelayTicks),
                ServerPlatform.ticks(periodTicks)
        ));
    }
}
