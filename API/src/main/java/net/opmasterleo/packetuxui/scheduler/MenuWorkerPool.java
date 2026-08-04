package net.opmasterleo.packetuxui.scheduler;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Dedicated bounded pool for menu build / materialize / preload work.
 * Keeps heavy GUI construction off the tick/entity thread and off the shared
 * Bukkit/Paper async scheduler. Safe on Folia when tasks do not touch world/entity
 * state — hop back with {@link PlatformScheduler#runForPlayer}.
 */
public final class MenuWorkerPool implements Executor {

    private static final AtomicInteger POOL_SEQ = new AtomicInteger();
    private static final AtomicInteger THREAD_SEQ = new AtomicInteger();

    private final ExecutorService executor;
    private final int threads;

    private MenuWorkerPool(ExecutorService executor, int threads) {
        this.executor = executor;
        this.threads = threads;
    }

    public static MenuWorkerPool create(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        int cpus = Runtime.getRuntime().availableProcessors();
        int threads = Math.max(2, Math.min(8, cpus));
        int poolId = POOL_SEQ.incrementAndGet();
        String pluginName = plugin.getName();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(
                    runnable,
                    "packetuxui-menu-" + pluginName + "-" + poolId + "-" + THREAD_SEQ.incrementAndGet()
            );
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        };
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                threads,
                threads,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(256),
                factory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        pool.allowCoreThreadTimeOut(true);
        return new MenuWorkerPool(pool, threads);
    }

    @Override
    public void execute(Runnable command) {
        Objects.requireNonNull(command, "command");
        executor.execute(command);
    }

    public Executor executor() {
        return executor;
    }

    public int threadCount() {
        return threads;
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2L, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
