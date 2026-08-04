package net.opmasterleo.packetuxui.scheduler;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Region-thread tasks — Folia/Paper {@code RegionScheduler}, Bukkit main thread.
 */
public interface RegionTasks {

    void runAt(Location location, Runnable task);

    void runAt(World world, int chunkX, int chunkZ, Runnable task);

    default void runAt(Block block, Runnable task) {
        runAt(block.getLocation(), task);
    }

    TaskHandle runAtNextTick(Location location, Runnable task);

    TaskHandle runAtNextTick(World world, int chunkX, int chunkZ, Runnable task);

    default TaskHandle runAtNextTick(Block block, Runnable task) {
        return runAtNextTick(block.getLocation(), task);
    }

    TaskHandle runAtLater(Location location, Runnable task, long delayTicks);

    TaskHandle runAtLater(World world, int chunkX, int chunkZ, Runnable task, long delayTicks);

    default TaskHandle runAtLater(Block block, Runnable task, long delayTicks) {
        return runAtLater(block.getLocation(), task, delayTicks);
    }

    TaskHandle runAtRepeating(Location location, Runnable task, long initialDelayTicks, long periodTicks);

    TaskHandle runAtRepeating(
            World world,
            int chunkX,
            int chunkZ,
            Runnable task,
            long initialDelayTicks,
            long periodTicks
    );

    default TaskHandle runAtRepeating(Block block, Runnable task, long initialDelayTicks, long periodTicks) {
        return runAtRepeating(block.getLocation(), task, initialDelayTicks, periodTicks);
    }

    static RegionTasks bukkit(GlobalTasks global) {
        return new net.opmasterleo.packetuxui.scheduler.bukkit.BukkitRegionTasks(global);
    }

    static RegionTasks paper(JavaPlugin plugin) {
        return new net.opmasterleo.packetuxui.scheduler.paper.PaperRegionTasks(plugin);
    }
}
