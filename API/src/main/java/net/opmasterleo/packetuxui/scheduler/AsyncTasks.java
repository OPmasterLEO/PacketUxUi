package net.opmasterleo.packetuxui.scheduler;

import java.util.concurrent.TimeUnit;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Async tasks — Folia/Paper {@code AsyncScheduler}, Bukkit async scheduler.
 * Do not touch Bukkit entity/world state here. Menu builds prefer {@link MenuWorkerPool}.
 */
public interface AsyncTasks {

    void run(Runnable task);

    TaskHandle runLater(Runnable task, long delay, TimeUnit unit);

    default TaskHandle runLaterTicks(Runnable task, long delayTicks) {
        return runLater(task, ServerPlatform.periodTicks(delayTicks) * 50L, TimeUnit.MILLISECONDS);
    }

    TaskHandle runRepeating(Runnable task, long initialDelay, long period, TimeUnit unit);

    default TaskHandle runRepeatingTicks(Runnable task, long initialDelayTicks, long periodTicks) {
        return runRepeating(
                task,
                ServerPlatform.periodTicks(initialDelayTicks) * 50L,
                ServerPlatform.periodTicks(periodTicks) * 50L,
                TimeUnit.MILLISECONDS
        );
    }

    static AsyncTasks bukkit(JavaPlugin plugin) {
        return new net.opmasterleo.packetuxui.scheduler.bukkit.BukkitAsyncTasks(plugin);
    }

    static AsyncTasks paper(JavaPlugin plugin) {
        return new net.opmasterleo.packetuxui.scheduler.paper.PaperAsyncTasks(plugin);
    }
}
