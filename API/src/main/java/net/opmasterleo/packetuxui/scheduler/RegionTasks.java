package net.opmasterleo.packetuxui.scheduler;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class RegionTasks {

    private final SchedulerEnvironment env;
    private final GlobalTasks global;

    RegionTasks(SchedulerEnvironment env, GlobalTasks global) {
        this.env = env;
        this.global = global;
    }

    public void runAt(Location location, Runnable task) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(task, "task");
        if (env.hasPaperSchedulers()) {
            if (env.isOwnedByCurrentRegion(location)) {
                task.run();
                return;
            }
            Bukkit.getRegionScheduler().execute(env.plugin(), location, task);
            return;
        }
        global.run(task);
    }

    public void runAt(World world, int chunkX, int chunkZ, Runnable task) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(task, "task");
        if (env.hasPaperSchedulers()) {
            if (env.isOwnedByCurrentRegion(world, chunkX, chunkZ)) {
                task.run();
                return;
            }
            Bukkit.getRegionScheduler().execute(env.plugin(), world, chunkX, chunkZ, task);
            return;
        }
        global.run(task);
    }

    public void runAt(Block block, Runnable task) {
        Objects.requireNonNull(block, "block");
        runAt(block.getLocation(), task);
    }

    public TaskHandle runAtNextTick(Location location, Runnable task) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(task, "task");
        if (env.hasPaperSchedulers()) {
            return TaskHandles.of(Bukkit.getRegionScheduler().run(env.plugin(), location, st -> task.run()));
        }
        return global.runNextTick(task);
    }

    public TaskHandle runAtLater(Location location, Runnable task, long delayTicks) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(task, "task");
        long delay = SchedulerEnvironment.ticks(delayTicks);
        if (env.hasPaperSchedulers()) {
            return TaskHandles.of(
                    Bukkit.getRegionScheduler().runDelayed(env.plugin(), location, st -> task.run(), delay)
            );
        }
        return global.runLater(task, delay);
    }

    public TaskHandle runAtLater(World world, int chunkX, int chunkZ, Runnable task, long delayTicks) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(task, "task");
        long delay = SchedulerEnvironment.ticks(delayTicks);
        if (env.hasPaperSchedulers()) {
            return TaskHandles.of(Bukkit.getRegionScheduler().runDelayed(
                    env.plugin(), world, chunkX, chunkZ, st -> task.run(), delay
            ));
        }
        return global.runLater(task, delay);
    }

    public TaskHandle runAtRepeating(Location location, Runnable task, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(task, "task");
        long delay = SchedulerEnvironment.ticks(initialDelayTicks);
        long period = SchedulerEnvironment.ticks(periodTicks);
        if (env.hasPaperSchedulers()) {
            return TaskHandles.of(Bukkit.getRegionScheduler().runAtFixedRate(
                    env.plugin(), location, st -> task.run(), delay, period
            ));
        }
        return global.runRepeating(task, delay, period);
    }

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
        long delay = SchedulerEnvironment.ticks(initialDelayTicks);
        long period = SchedulerEnvironment.ticks(periodTicks);
        if (env.hasPaperSchedulers()) {
            return TaskHandles.of(Bukkit.getRegionScheduler().runAtFixedRate(
                    env.plugin(), world, chunkX, chunkZ, st -> task.run(), delay, period
            ));
        }
        return global.runRepeating(task, delay, period);
    }
}
