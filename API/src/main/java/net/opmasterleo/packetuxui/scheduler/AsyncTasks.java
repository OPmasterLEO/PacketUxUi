package net.opmasterleo.packetuxui.scheduler;

import java.util.concurrent.TimeUnit;

import org.bukkit.plugin.java.JavaPlugin;

public interface AsyncTasks {

    void run(Runnable task);

    TaskHandle runLater(Runnable task, long delay, TimeUnit unit);

    default TaskHandle runLaterTicks(Runnable task, long delayTicks) {
        return runLater(task, ServerPlatform.ticks(delayTicks) * 50L, TimeUnit.MILLISECONDS);
    }

    TaskHandle runRepeating(Runnable task, long initialDelay, long period, TimeUnit unit);

    default TaskHandle runRepeatingTicks(Runnable task, long initialDelayTicks, long periodTicks) {
        return runRepeating(
                task,
                ServerPlatform.ticks(initialDelayTicks) * 50L,
                ServerPlatform.ticks(periodTicks) * 50L,
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
