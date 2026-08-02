package net.opmasterleo.packetuxui.scheduler;

import java.util.Objects;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

final class EntityTasks {

    private final SchedulerEnvironment env;

    EntityTasks(SchedulerEnvironment env) {
        this.env = env;
    }

    void run(Entity entity, Runnable task, Runnable retired) {
        Objects.requireNonNull(task, "task");
        if (entity == null || !entity.isValid()) {
            if (retired != null) {
                retired.run();
            }
            return;
        }
        if (env.hasPaperSchedulers()) {
            if (env.isOwnedByCurrentRegion(entity)) {
                task.run();
                return;
            }
            entity.getScheduler().execute(env.plugin(), task, retired, 0L);
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(env.plugin(), task);
        }
    }

    TaskHandle runLater(Entity entity, Runnable task, Runnable retired, long delayTicks) {
        Objects.requireNonNull(task, "task");
        if (entity == null || !entity.isValid()) {
            if (retired != null) {
                retired.run();
            }
            return TaskHandle.NOOP;
        }
        long delay = SchedulerEnvironment.ticks(delayTicks);
        if (env.hasPaperSchedulers()) {
            ScheduledTask scheduled = entity.getScheduler().runDelayed(env.plugin(), st -> task.run(), retired, delay);
            return TaskHandles.of(scheduled);
        }
        return TaskHandles.of(Bukkit.getScheduler().runTaskLater(env.plugin(), task, delay));
    }

    TaskHandle runRepeating(
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
        long delay = SchedulerEnvironment.ticks(initialDelayTicks);
        long period = SchedulerEnvironment.ticks(periodTicks);
        if (env.hasPaperSchedulers()) {
            ScheduledTask scheduled = entity.getScheduler().runAtFixedRate(
                    env.plugin(),
                    st -> task.accept(entity),
                    retired,
                    delay,
                    period
            );
            return TaskHandles.of(scheduled);
        }
        return TaskHandles.of(Bukkit.getScheduler().runTaskTimer(env.plugin(), () -> {
            if (entity.isValid()) {
                task.accept(entity);
            }
        }, delay, period), true);
    }

    TaskHandle runLaterForPlayer(Player player, Consumer<Player> task, long delayTicks) {
        Objects.requireNonNull(task, "task");
        return runLater(player, () -> {
            if (player != null && player.isOnline()) {
                task.accept(player);
            }
        }, null, delayTicks);
    }

    TaskHandle runRepeatingForPlayer(
            Player player,
            Consumer<Player> task,
            long initialDelayTicks,
            long periodTicks
    ) {
        Objects.requireNonNull(task, "task");
        return runRepeating(player, entity -> {
            if (entity instanceof Player online && online.isOnline()) {
                task.accept(online);
            }
        }, null, initialDelayTicks, periodTicks);
    }
}
