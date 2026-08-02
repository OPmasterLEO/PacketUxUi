package net.opmasterleo.packetuxui.scheduler;

import java.util.Objects;

import org.bukkit.Bukkit;

final class GlobalTasks {

    private final SchedulerEnvironment env;

    GlobalTasks(SchedulerEnvironment env) {
        this.env = env;
    }

    void run(Runnable task) {
        Objects.requireNonNull(task, "task");
        if (env.hasPaperSchedulers()) {
            if (env.isGlobalTickThread()) {
                task.run();
                return;
            }
            Bukkit.getGlobalRegionScheduler().execute(env.plugin(), task);
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(env.plugin(), task);
        }
    }

    TaskHandle runNextTick(Runnable task) {
        Objects.requireNonNull(task, "task");
        if (env.hasPaperSchedulers()) {
            return TaskHandles.of(Bukkit.getGlobalRegionScheduler().run(env.plugin(), st -> task.run()));
        }
        return TaskHandles.of(Bukkit.getScheduler().runTask(env.plugin(), task));
    }

    TaskHandle runLater(Runnable task, long delayTicks) {
        Objects.requireNonNull(task, "task");
        long delay = SchedulerEnvironment.ticks(delayTicks);
        if (env.hasPaperSchedulers()) {
            return TaskHandles.of(Bukkit.getGlobalRegionScheduler().runDelayed(env.plugin(), st -> task.run(), delay));
        }
        return TaskHandles.of(Bukkit.getScheduler().runTaskLater(env.plugin(), task, delay));
    }

    TaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(task, "task");
        long delay = SchedulerEnvironment.ticks(initialDelayTicks);
        long period = SchedulerEnvironment.ticks(periodTicks);
        if (env.hasPaperSchedulers()) {
            return TaskHandles.of(
                    Bukkit.getGlobalRegionScheduler().runAtFixedRate(env.plugin(), st -> task.run(), delay, period)
            );
        }
        return TaskHandles.of(Bukkit.getScheduler().runTaskTimer(env.plugin(), task, delay, period), true);
    }
}
