package net.opmasterleo.packetuxui.scheduler.paper;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import net.opmasterleo.packetuxui.scheduler.RegionTasks;
import net.opmasterleo.packetuxui.scheduler.ServerPlatform;
import net.opmasterleo.packetuxui.scheduler.TaskHandle;

public final class PaperRegionTasks implements RegionTasks {

    private final JavaPlugin plugin;

    public PaperRegionTasks(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void runAt(Location location, Runnable task) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(task, "task");
        if (location.getWorld() != null && Bukkit.isOwnedByCurrentRegion(location)) {
            task.run();
            return;
        }
        Bukkit.getRegionScheduler().execute(plugin, location, task);
    }

    @Override
    public void runAt(World world, int chunkX, int chunkZ, Runnable task) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(task, "task");
        if (Bukkit.isOwnedByCurrentRegion(world, chunkX, chunkZ)) {
            task.run();
            return;
        }
        Bukkit.getRegionScheduler().execute(plugin, world, chunkX, chunkZ, task);
    }

    @Override
    public TaskHandle runAtNextTick(Location location, Runnable task) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(task, "task");
        return PaperTaskHandles.of(Bukkit.getRegionScheduler().run(plugin, location, PaperConsumers.run(task)));
    }

    @Override
    public TaskHandle runAtNextTick(World world, int chunkX, int chunkZ, Runnable task) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(task, "task");
        return PaperTaskHandles.of(
                Bukkit.getRegionScheduler().run(plugin, world, chunkX, chunkZ, PaperConsumers.run(task))
        );
    }

    @Override
    public TaskHandle runAtLater(Location location, Runnable task, long delayTicks) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(task, "task");
        long delay = ServerPlatform.delayTicks(delayTicks);
        if (delay <= 0L) {
            return runAtNextTick(location, task);
        }
        return PaperTaskHandles.of(Bukkit.getRegionScheduler().runDelayed(
                plugin, location, PaperConsumers.run(task), delay
        ));
    }

    @Override
    public TaskHandle runAtLater(World world, int chunkX, int chunkZ, Runnable task, long delayTicks) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(task, "task");
        long delay = ServerPlatform.delayTicks(delayTicks);
        if (delay <= 0L) {
            return runAtNextTick(world, chunkX, chunkZ, task);
        }
        return PaperTaskHandles.of(Bukkit.getRegionScheduler().runDelayed(
                plugin, world, chunkX, chunkZ, PaperConsumers.run(task), delay
        ));
    }

    @Override
    public TaskHandle runAtRepeating(Location location, Runnable task, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(task, "task");
        return PaperTaskHandles.of(Bukkit.getRegionScheduler().runAtFixedRate(
                plugin,
                location,
                PaperConsumers.run(task),
                ServerPlatform.periodTicks(initialDelayTicks),
                ServerPlatform.periodTicks(periodTicks)
        ));
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
        return PaperTaskHandles.of(Bukkit.getRegionScheduler().runAtFixedRate(
                plugin,
                world,
                chunkX,
                chunkZ,
                PaperConsumers.run(task),
                ServerPlatform.periodTicks(initialDelayTicks),
                ServerPlatform.periodTicks(periodTicks)
        ));
    }
}
