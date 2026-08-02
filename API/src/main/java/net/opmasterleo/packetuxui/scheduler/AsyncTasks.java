package net.opmasterleo.packetuxui.scheduler;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;

public final class AsyncTasks {

    private final SchedulerEnvironment env;

    AsyncTasks(SchedulerEnvironment env) {
        this.env = env;
    }

    public void run(Runnable task) {
        Objects.requireNonNull(task, "task");
        if (env.hasPaperSchedulers()) {
            Bukkit.getAsyncScheduler().runNow(env.plugin(), st -> task.run());
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(env.plugin(), task);
    }

    public TaskHandle runLater(Runnable task, long delay, TimeUnit unit) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(unit, "unit");
        long safeDelay = Math.max(1L, delay);
        if (env.hasPaperSchedulers()) {
            return TaskHandles.of(
                    Bukkit.getAsyncScheduler().runDelayed(env.plugin(), st -> task.run(), safeDelay, unit)
            );
        }
        long ticks = Math.max(1L, unit.toMillis(safeDelay) / 50L);
        return TaskHandles.of(Bukkit.getScheduler().runTaskLaterAsynchronously(env.plugin(), task, ticks));
    }

    public TaskHandle runLaterTicks(Runnable task, long delayTicks) {
        return runLater(task, SchedulerEnvironment.ticks(delayTicks) * 50L, TimeUnit.MILLISECONDS);
    }

    public TaskHandle runRepeating(Runnable task, long initialDelay, long period, TimeUnit unit) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(unit, "unit");
        long delay = Math.max(1L, initialDelay);
        long rate = Math.max(1L, period);
        if (env.hasPaperSchedulers()) {
            return TaskHandles.of(
                    Bukkit.getAsyncScheduler().runAtFixedRate(env.plugin(), st -> task.run(), delay, rate, unit)
            );
        }
        long delayTicks = Math.max(1L, unit.toMillis(delay) / 50L);
        long periodTicks = Math.max(1L, unit.toMillis(rate) / 50L);
        return TaskHandles.of(
                Bukkit.getScheduler().runTaskTimerAsynchronously(env.plugin(), task, delayTicks, periodTicks),
                true
        );
    }

    public TaskHandle runRepeatingTicks(Runnable task, long initialDelayTicks, long periodTicks) {
        return runRepeating(
                task,
                SchedulerEnvironment.ticks(initialDelayTicks) * 50L,
                SchedulerEnvironment.ticks(periodTicks) * 50L,
                TimeUnit.MILLISECONDS
        );
    }
}
