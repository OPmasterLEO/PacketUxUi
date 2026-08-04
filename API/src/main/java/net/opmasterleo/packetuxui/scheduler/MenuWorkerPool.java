package net.opmasterleo.packetuxui.scheduler;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Elastic, CPU-sized menu worker pool with idle reclaim and leak-safe shutdown.
 * <p>
 * Sizing (override with {@code -Dpacketuxui.menuWorkers.max=N}):
 * core starts small and scales to a CPU-derived max; all threads (including core)
 * time out when idle so the pool returns to zero workers under no load.
 * <p>
 * Queue uses a scaling handoff so threads grow under load instead of filling a
 * large queue first (classic {@link ThreadPoolExecutor} pitfall).
 */
public final class MenuWorkerPool implements Executor, AutoCloseable {

    private static final AtomicInteger POOL_SEQ = new AtomicInteger();
    private static final AtomicInteger THREAD_SEQ = new AtomicInteger();

    private static final long KEEP_ALIVE_SECONDS = 15L;
    private static final long SHUTDOWN_WAIT_MS = 2500L;
    private static final int MAX_WORKERS_CAP = 16;
    private static final int MIN_WORKERS = 1;

    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicLong submitted = new AtomicLong();
    private final AtomicLong completed = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();

    private final int coreThreads;
    private final int maxThreads;
    private final int queueCapacity;
    private final ThreadPoolExecutor executor;
    private final ClassLoader taskClassLoader;
    private final Logger logger;
    private final String label;

    private MenuWorkerPool(
            ThreadPoolExecutor executor,
            int coreThreads,
            int maxThreads,
            int queueCapacity,
            ClassLoader taskClassLoader,
            Logger logger,
            String label
    ) {
        this.executor = executor;
        this.coreThreads = coreThreads;
        this.maxThreads = maxThreads;
        this.queueCapacity = queueCapacity;
        this.taskClassLoader = taskClassLoader;
        this.logger = logger;
        this.label = label;
    }

    public static MenuWorkerPool create(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        Sizing sizing = Sizing.fromRuntime();
        int poolId = POOL_SEQ.incrementAndGet();
        String pluginName = plugin.getName();
        String label = "packetuxui-menu-" + pluginName + "-" + poolId;
        Logger logger = plugin.getLogger();
        ClassLoader taskClassLoader = plugin.getClass().getClassLoader();

        ScalingQueue queue = new ScalingQueue(sizing.queueCapacity);
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, label + "-" + THREAD_SEQ.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            // Idle workers must not pin the plugin ClassLoader.
            thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
            return thread;
        };

        LeakSafeRejection rejection = new LeakSafeRejection(logger, label);
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                sizing.coreThreads,
                sizing.maxThreads,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                queue,
                factory,
                rejection
        );
        queue.bind(pool);
        // Core threads expire too — pool can shrink to 0 when idle.
        pool.allowCoreThreadTimeOut(true);

        MenuWorkerPool workers = new MenuWorkerPool(
                pool,
                sizing.coreThreads,
                sizing.maxThreads,
                sizing.queueCapacity,
                taskClassLoader,
                logger,
                label
        );
        rejection.bind(workers);
        return workers;
    }

    /** CPU-derived sizing; system properties optional. */
    static final class Sizing {
        final int coreThreads;
        final int maxThreads;
        final int queueCapacity;

        Sizing(int coreThreads, int maxThreads, int queueCapacity) {
            this.coreThreads = coreThreads;
            this.maxThreads = maxThreads;
            this.queueCapacity = queueCapacity;
        }

        static Sizing fromRuntime() {
            return fromCpus(Runtime.getRuntime().availableProcessors());
        }

        static Sizing fromCpus(int rawCpus) {
            int cpus = Math.max(1, rawCpus);
            int autoMax = autoMaxWorkers(cpus);
            int max = clamp(parseIntProp("packetuxui.menuWorkers.max", autoMax), MIN_WORKERS, MAX_WORKERS_CAP);
            int autoCore = Math.min(max, Math.max(MIN_WORKERS, (cpus + 3) / 4));
            int core = clamp(parseIntProp("packetuxui.menuWorkers.core", autoCore), MIN_WORKERS, max);
            int autoQueue = Math.max(64, max * 48);
            int queue = Math.max(16, parseIntProp("packetuxui.menuWorkers.queue", autoQueue));
            return new Sizing(core, max, queue);
        }

        /**
         * Low-core: use all CPUs. Mid: leave headroom for tick/Netty.
         * High: sub-linear scale, hard-capped — menu builds are bursty, not HPC.
         */
        static int autoMaxWorkers(int cpus) {
            if (cpus <= 2) {
                return Math.max(MIN_WORKERS, cpus);
            }
            if (cpus <= 4) {
                return cpus;
            }
            if (cpus <= 8) {
                return Math.max(4, cpus - 1);
            }
            return Math.min(MAX_WORKERS_CAP, Math.max(6, (cpus * 3) / 4));
        }

        private static int parseIntProp(String key, int fallback) {
            try {
                String raw = System.getProperty(key);
                if (raw == null || raw.isBlank()) {
                    return fallback;
                }
                return Integer.parseInt(raw.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    /**
     * Prefer spawning workers under load; only enqueue when at max pool size
     * or when an idle worker can take the task immediately.
     */
    static final class ScalingQueue extends LinkedBlockingQueue<Runnable> {
        private volatile ThreadPoolExecutor owner;

        ScalingQueue(int capacity) {
            super(capacity);
        }

        void bind(ThreadPoolExecutor owner) {
            this.owner = owner;
        }

        @Override
        public boolean offer(Runnable task) {
            ThreadPoolExecutor pool = owner;
            if (pool == null) {
                return super.offer(task);
            }
            int poolSize = pool.getPoolSize();
            int active = pool.getActiveCount();
            if (active < poolSize) {
                return super.offer(task);
            }
            if (poolSize < pool.getMaximumPoolSize()) {
                return false;
            }
            return super.offer(task);
        }
    }

    private static final class LeakSafeRejection implements RejectedExecutionHandler {
        private final Logger logger;
        private final String label;
        private volatile MenuWorkerPool pool;

        LeakSafeRejection(Logger logger, String label) {
            this.logger = logger;
            this.label = label;
        }

        void bind(MenuWorkerPool pool) {
            this.pool = pool;
        }

        @Override
        public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
            MenuWorkerPool workers = pool;
            if (workers != null) {
                workers.rejected.incrementAndGet();
            }
            if (executor.isShutdown() || (workers != null && workers.closed.get())) {
                return;
            }
            if (logger != null) {
                logger.log(
                        Level.WARNING,
                        "[PacketUxUi] " + label + " saturated (max workers + full queue); "
                                + "rejecting menu build to protect tick/entity threads"
                );
            }
            throw new RejectedExecutionException(label + " saturated");
        }
    }

    @Override
    public void execute(Runnable command) {
        Objects.requireNonNull(command, "command");
        if (!isAvailable()) {
            return;
        }
        submitted.incrementAndGet();
        inFlight.incrementAndGet();
        try {
            executor.execute(wrap(command));
        } catch (RejectedExecutionException ignored) {
            inFlight.decrementAndGet();
        } catch (Throwable error) {
            inFlight.decrementAndGet();
            throw error;
        }
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        if (!isAvailable()) {
            return CompletableFuture.failedFuture(new RejectedExecutionException(label + " closed"));
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        submitted.incrementAndGet();
        inFlight.incrementAndGet();
        try {
            executor.execute(wrap(() -> {
                if (closed.get()) {
                    future.completeExceptionally(new RejectedExecutionException(label + " closed"));
                    return;
                }
                try {
                    future.complete(supplier.get());
                } catch (Throwable error) {
                    future.completeExceptionally(error);
                }
            }));
        } catch (RejectedExecutionException rejected) {
            inFlight.decrementAndGet();
            future.completeExceptionally(rejected);
        } catch (Throwable error) {
            inFlight.decrementAndGet();
            future.completeExceptionally(error);
        }
        return future;
    }

    private Runnable wrap(Runnable command) {
        return () -> {
            Thread thread = Thread.currentThread();
            ClassLoader previous = thread.getContextClassLoader();
            try {
                thread.setContextClassLoader(taskClassLoader);
                if (!closed.get()) {
                    command.run();
                }
            } catch (Throwable error) {
                if (logger != null) {
                    logger.log(Level.SEVERE, "[PacketUxUi] Uncaught error in " + label, error);
                }
            } finally {
                thread.setContextClassLoader(previous != null ? previous : ClassLoader.getSystemClassLoader());
                inFlight.decrementAndGet();
                completed.incrementAndGet();
            }
        };
    }

    public Executor executor() {
        return this;
    }

    public boolean isClosed() {
        return closed.get();
    }

    public boolean isAvailable() {
        return !closed.get() && !executor.isShutdown();
    }

    /** Configured max workers (CPU-derived unless overridden). */
    public int threadCount() {
        return maxThreads;
    }

    public int coreThreadCount() {
        return coreThreads;
    }

    public int maxThreadCount() {
        return maxThreads;
    }

    public int queueCapacity() {
        return queueCapacity;
    }

    public int activeThreads() {
        return executor.getActiveCount();
    }

    public int poolSize() {
        return executor.getPoolSize();
    }

    public int queuedTasks() {
        return executor.getQueue().size();
    }

    public int inFlightTasks() {
        return Math.max(0, inFlight.get());
    }

    public long submittedTasks() {
        return submitted.get();
    }

    public long completedTasks() {
        return completed.get();
    }

    public long rejectedTasks() {
        return rejected.get();
    }

    public String diagnostics() {
        return label
                + "{core=" + coreThreads
                + ",max=" + maxThreads
                + ",queueCap=" + queueCapacity
                + ",pool=" + poolSize()
                + ",active=" + activeThreads()
                + ",queued=" + queuedTasks()
                + ",inFlight=" + inFlightTasks()
                + ",done=" + completedTasks()
                + ",rejected=" + rejectedTasks()
                + ",closed=" + closed.get()
                + "}";
    }

    @Override
    public void close() {
        shutdown();
    }

    /**
     * Idempotent: stop accepting work, interrupt workers, drain queue, await idle,
     * then purge so nothing retains runnables (player/menu closures).
     */
    public void shutdown() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_WAIT_MS, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
                executor.awaitTermination(SHUTDOWN_WAIT_MS, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            executor.getQueue().clear();
            executor.purge();
            inFlight.set(0);
        }
    }
}
