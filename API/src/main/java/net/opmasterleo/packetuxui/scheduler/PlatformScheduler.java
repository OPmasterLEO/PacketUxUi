package net.opmasterleo.packetuxui.scheduler;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlatformScheduler {

    private final SchedulerEnvironment env;
    private final GlobalTasks global;
    private final EntityTasks entity;
    private final RegionTasks region;
    private final AsyncTasks async;

    public PlatformScheduler(JavaPlugin plugin) {
        this.env = new SchedulerEnvironment(Objects.requireNonNull(plugin, "plugin"));
        this.global = new GlobalTasks(env);
        this.entity = new EntityTasks(env);
        this.region = new RegionTasks(env, global);
        this.async = new AsyncTasks(env);
    }

    public JavaPlugin plugin() {
        return env.plugin();
    }

    public boolean isFolia() {
        return env.isFolia();
    }

    public boolean hasPaperSchedulers() {
        return env.hasPaperSchedulers();
    }

    public boolean isPrimaryThread() {
        return env.isPrimaryThread();
    }

    public boolean isGlobalTickThread() {
        return env.isGlobalTickThread();
    }

    public boolean isOwnedByCurrentRegion(Entity entity) {
        return env.isOwnedByCurrentRegion(entity);
    }

    public boolean isOwnedByCurrentRegion(Location location) {
        return env.isOwnedByCurrentRegion(location);
    }

    public boolean isOwnedByCurrentRegion(Block block) {
        return env.isOwnedByCurrentRegion(block);
    }

    public boolean isOwnedByCurrentRegion(World world, int chunkX, int chunkZ) {
        return env.isOwnedByCurrentRegion(world, chunkX, chunkZ);
    }

    public void runSync(Runnable task) {
        global.run(task);
    }

    public TaskHandle runSyncLater(Runnable task, long delayTicks) {
        return global.runLater(task, delayTicks);
    }

    public TaskHandle runSyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        return global.runRepeating(task, delayTicks, periodTicks);
    }

    public void runGlobal(Runnable task) {
        global.run(task);
    }

    public TaskHandle runGlobalNextTick(Runnable task) {
        return global.runNextTick(task);
    }

    public TaskHandle runGlobalLater(Runnable task, long delayTicks) {
        return global.runLater(task, delayTicks);
    }

    public TaskHandle runRepeatingGlobal(Runnable task, long periodTicks) {
        return global.runRepeating(task, periodTicks, periodTicks);
    }

    public TaskHandle runRepeatingGlobal(Runnable task, long initialDelayTicks, long periodTicks) {
        return global.runRepeating(task, initialDelayTicks, periodTicks);
    }

    public void runForPlayer(Player player, Runnable task) {
        entity.run(player, task, null);
    }

    public void runForPlayer(Player player, Runnable task, Runnable retired) {
        entity.run(player, task, retired);
    }

    public void run(Entity target, Runnable task) {
        entity.run(target, task, null);
    }

    public void run(Entity target, Runnable task, Runnable retired) {
        entity.run(target, task, retired);
    }

    public TaskHandle runNextTick(Entity target, Runnable task) {
        return entity.runLater(target, task, null, 1L);
    }

    public TaskHandle runLater(Entity target, Runnable task, long delayTicks) {
        return entity.runLater(target, task, null, delayTicks);
    }

    public TaskHandle runLater(Entity target, Runnable task, Runnable retired, long delayTicks) {
        return entity.runLater(target, task, retired, delayTicks);
    }

    public TaskHandle runRepeating(Entity target, Consumer<? super Entity> task, long periodTicks) {
        return entity.runRepeating(target, task, null, periodTicks, periodTicks);
    }

    public TaskHandle runRepeating(
            Entity target,
            Consumer<? super Entity> task,
            long initialDelayTicks,
            long periodTicks
    ) {
        return entity.runRepeating(target, task, null, initialDelayTicks, periodTicks);
    }

    public TaskHandle runRepeating(
            Entity target,
            Consumer<? super Entity> task,
            Runnable retired,
            long initialDelayTicks,
            long periodTicks
    ) {
        return entity.runRepeating(target, task, retired, initialDelayTicks, periodTicks);
    }

    public TaskHandle runRepeatingForPlayer(Player player, Consumer<Player> task, long periodTicks) {
        return entity.runRepeatingForPlayer(player, task, periodTicks, periodTicks);
    }

    public TaskHandle runRepeatingForPlayer(
            Player player,
            Consumer<Player> task,
            long initialDelayTicks,
            long periodTicks
    ) {
        return entity.runRepeatingForPlayer(player, task, initialDelayTicks, periodTicks);
    }

    public TaskHandle runLaterForPlayer(Player player, Consumer<Player> task, long delayTicks) {
        return entity.runLaterForPlayer(player, task, delayTicks);
    }

    public void runAt(Location location, Runnable task) {
        region.runAt(location, task);
    }

    public void runAt(World world, int chunkX, int chunkZ, Runnable task) {
        region.runAt(world, chunkX, chunkZ, task);
    }

    public void runAt(Block block, Runnable task) {
        region.runAt(block, task);
    }

    public TaskHandle runAtNextTick(Location location, Runnable task) {
        return region.runAtNextTick(location, task);
    }

    public TaskHandle runAtLater(Location location, Runnable task, long delayTicks) {
        return region.runAtLater(location, task, delayTicks);
    }

    public TaskHandle runAtLater(World world, int chunkX, int chunkZ, Runnable task, long delayTicks) {
        return region.runAtLater(world, chunkX, chunkZ, task, delayTicks);
    }

    public TaskHandle runAtRepeating(Location location, Runnable task, long periodTicks) {
        return region.runAtRepeating(location, task, periodTicks, periodTicks);
    }

    public TaskHandle runAtRepeating(Location location, Runnable task, long initialDelayTicks, long periodTicks) {
        return region.runAtRepeating(location, task, initialDelayTicks, periodTicks);
    }

    public TaskHandle runAtRepeating(
            World world,
            int chunkX,
            int chunkZ,
            Runnable task,
            long initialDelayTicks,
            long periodTicks
    ) {
        return region.runAtRepeating(world, chunkX, chunkZ, task, initialDelayTicks, periodTicks);
    }

    public void runAsync(Runnable task) {
        async.run(task);
    }

    public TaskHandle runAsyncLater(Runnable task, long delay, TimeUnit unit) {
        return async.runLater(task, delay, unit);
    }

    public TaskHandle runAsyncLaterTicks(Runnable task, long delayTicks) {
        return async.runLaterTicks(task, delayTicks);
    }

    public TaskHandle runAsyncRepeating(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return async.runRepeating(task, initialDelay, period, unit);
    }

    public TaskHandle runAsyncRepeatingTicks(Runnable task, long initialDelayTicks, long periodTicks) {
        return async.runRepeatingTicks(task, initialDelayTicks, periodTicks);
    }

    public void cancelAll() {
        env.cancelAll();
    }
}
