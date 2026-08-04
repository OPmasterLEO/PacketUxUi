package net.opmasterleo.packetuxui.scheduler;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlatformScheduler {

    private final ServerPlatform platform;
    private final GlobalTasks global;
    private final EntityTasks entity;
    private final RegionTasks region;
    private final AsyncTasks async;
    private final MenuWorkerPool menuWorkers;

    public PlatformScheduler(JavaPlugin plugin) {
        this(ServerPlatform.detect(Objects.requireNonNull(plugin, "plugin")));
    }

    public PlatformScheduler(ServerPlatform platform) {
        this.platform = Objects.requireNonNull(platform, "platform");
        JavaPlugin plugin = platform.plugin();
        if (platform.isPaper()) {
            this.global = GlobalTasks.paper(plugin);
            this.entity = EntityTasks.paper(plugin);
            this.region = RegionTasks.paper(plugin);
            this.async = AsyncTasks.paper(plugin);
        } else {
            this.global = GlobalTasks.bukkit(plugin);
            this.entity = EntityTasks.bukkit(plugin);
            this.region = RegionTasks.bukkit(this.global);
            this.async = AsyncTasks.bukkit(plugin);
        }
        this.menuWorkers = MenuWorkerPool.create(plugin);
    }

    public JavaPlugin plugin() {
        return platform.plugin();
    }

    public ServerPlatform platform() {
        return platform;
    }

    public SchedulerKind kind() {
        return platform.kind();
    }

    public boolean isFolia() {
        return platform.isFolia();
    }

    public boolean hasPaperSchedulers() {
        return platform.isPaper();
    }

    public boolean isPrimaryThread() {
        return Bukkit.isPrimaryThread();
    }

    public boolean isGlobalTickThread() {
        if (!platform.isPaper()) {
            return Bukkit.isPrimaryThread();
        }
        try {
            return Bukkit.getServer().isGlobalTickThread();
        } catch (Throwable ignored) {
            return Bukkit.isPrimaryThread();
        }
    }

    public boolean isOwnedByCurrentRegion(Entity entity) {
        if (!platform.isPaper() || entity == null) {
            return Bukkit.isPrimaryThread();
        }
        try {
            return Bukkit.isOwnedByCurrentRegion(entity);
        } catch (Throwable ignored) {
            return Bukkit.isPrimaryThread();
        }
    }

    public boolean isOwnedByCurrentRegion(Location location) {
        if (!platform.isPaper() || location == null || location.getWorld() == null) {
            return Bukkit.isPrimaryThread();
        }
        try {
            return Bukkit.isOwnedByCurrentRegion(location);
        } catch (Throwable ignored) {
            return Bukkit.isPrimaryThread();
        }
    }

    public boolean isOwnedByCurrentRegion(Block block) {
        if (!platform.isPaper() || block == null) {
            return Bukkit.isPrimaryThread();
        }
        try {
            return Bukkit.isOwnedByCurrentRegion(block);
        } catch (Throwable ignored) {
            return Bukkit.isPrimaryThread();
        }
    }

    public boolean isOwnedByCurrentRegion(World world, int chunkX, int chunkZ) {
        if (!platform.isPaper() || world == null) {
            return Bukkit.isPrimaryThread();
        }
        try {
            return Bukkit.isOwnedByCurrentRegion(world, chunkX, chunkZ);
        } catch (Throwable ignored) {
            return Bukkit.isPrimaryThread();
        }
    }

    public GlobalTasks global() {
        return global;
    }

    public EntityTasks entity() {
        return entity;
    }

    public RegionTasks region() {
        return region;
    }

    public AsyncTasks async() {
        return async;
    }

    /**
     * Dedicated bounded pool for menu build/materialize/preload (Paper, Folia, Spigot).
     * Do not touch Bukkit player/world state here — hop with {@link #runForPlayer}.
     */
    public MenuWorkerPool menuWorkers() {
        return menuWorkers;
    }

    public Executor menuExecutor() {
        return menuWorkers.executor();
    }

    public void runMenuAsync(Runnable task) {
        menuWorkers.execute(task);
    }

    public <T> CompletableFuture<T> supplyMenuAsync(Supplier<T> supplier) {
        return menuWorkers.supplyAsync(Objects.requireNonNull(supplier, "supplier"));
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
        JavaPlugin plugin = platform.plugin();
        if (platform.isPaper()) {
            try {
                Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
            } catch (Throwable ignored) {
            }
            try {
                Bukkit.getAsyncScheduler().cancelTasks(plugin);
            } catch (Throwable ignored) {
            }
        }
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    /** Cancel platform tasks and shut down the dedicated menu worker pool. */
    public void shutdown() {
        cancelAll();
        menuWorkers.shutdown();
    }
}
