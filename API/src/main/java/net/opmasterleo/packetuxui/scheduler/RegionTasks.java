package net.opmasterleo.packetuxui.scheduler;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

public interface RegionTasks {

    void runAt(Location location, Runnable task);

    void runAt(World world, int chunkX, int chunkZ, Runnable task);

    default void runAt(Block block, Runnable task) {
        runAt(block.getLocation(), task);
    }

    TaskHandle runAtNextTick(Location location, Runnable task);

    TaskHandle runAtLater(Location location, Runnable task, long delayTicks);

    TaskHandle runAtLater(World world, int chunkX, int chunkZ, Runnable task, long delayTicks);

    TaskHandle runAtRepeating(Location location, Runnable task, long initialDelayTicks, long periodTicks);

    TaskHandle runAtRepeating(
            World world,
            int chunkX,
            int chunkZ,
            Runnable task,
            long initialDelayTicks,
            long periodTicks
    );

    static RegionTasks bukkit(GlobalTasks global) {
        return new net.opmasterleo.packetuxui.scheduler.bukkit.BukkitRegionTasks(global);
    }

    static RegionTasks paper(JavaPlugin plugin) {
        return new net.opmasterleo.packetuxui.scheduler.paper.PaperRegionTasks(plugin);
    }
}
