package net.opmasterleo.packetuxui.scheduler;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Global-region tasks — Folia/Paper {@code GlobalRegionScheduler}, Bukkit main thread.
 * <p>
 * Not for player inventory / entity state on Folia — use {@link EntityTasks}.
 */
public interface GlobalTasks {

    /** Run ASAP on the global region (inline if already on the global tick thread). */
    void run(Runnable task);

    /** Folia {@code GlobalRegionScheduler.run} — next global tick, cancellable. */
    TaskHandle runNextTick(Runnable task);

    TaskHandle runLater(Runnable task, long delayTicks);

    TaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks);

    static GlobalTasks bukkit(JavaPlugin plugin) {
        return new net.opmasterleo.packetuxui.scheduler.bukkit.BukkitGlobalTasks(plugin);
    }

    static GlobalTasks paper(JavaPlugin plugin) {
        return new net.opmasterleo.packetuxui.scheduler.paper.PaperGlobalTasks(plugin);
    }
}
