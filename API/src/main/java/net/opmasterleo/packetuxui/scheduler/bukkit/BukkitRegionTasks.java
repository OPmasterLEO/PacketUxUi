package net.opmasterleo.packetuxui.scheduler.bukkit;

import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.World;

import net.opmasterleo.packetuxui.scheduler.GlobalTasks;
import net.opmasterleo.packetuxui.scheduler.RegionTasks;
import net.opmasterleo.packetuxui.scheduler.TaskHandle;

public final class BukkitRegionTasks implements RegionTasks {

    private final GlobalTasks global;

    public BukkitRegionTasks(GlobalTasks global) {
        this.global = Objects.requireNonNull(global, "global");
    }

    @Override
    public void runAt(Location location, Runnable task) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(task, "task");
        global.run(task);
    }

    @Override
    public void runAt(World world, int chunkX, int chunkZ, Runnable task) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(task, "task");
        global.run(task);
    }

    @Override
    public TaskHandle runAtNextTick(Location location, Runnable task) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(task, "task");
        return global.runNextTick(task);
    }

    @Override
    public TaskHandle runAtLater(Location location, Runnable task, long delayTicks) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(task, "task");
        return global.runLater(task, delayTicks);
    }

    @Override
    public TaskHandle runAtLater(World world, int chunkX, int chunkZ, Runnable task, long delayTicks) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(task, "task");
        return global.runLater(task, delayTicks);
    }

    @Override
    public TaskHandle runAtRepeating(Location location, Runnable task, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(task, "task");
        return global.runRepeating(task, initialDelayTicks, periodTicks);
    }

    @Override
    public TaskHandle runAtRepeating(
            World world,
            int chunkX,
            int chunkZ,
            Runnable task,
            long initialDelayTicks,
            long periodTicks
    ) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(task, "task");
        return global.runRepeating(task, initialDelayTicks, periodTicks);
    }
}
