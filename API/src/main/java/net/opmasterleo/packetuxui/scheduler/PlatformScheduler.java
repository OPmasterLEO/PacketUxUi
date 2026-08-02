package net.opmasterleo.packetuxui.scheduler;

import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.key.Key;

public final class PlatformScheduler {

    private static final Key FOLIA_BRAND = Key.key("papermc", "folia");

    private final JavaPlugin plugin;
    private final boolean folia;

    public PlatformScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folia = detectFolia();
    }

    public boolean isFolia() {
        return folia;
    }

    public void runForPlayer(Player player, Runnable task) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (folia) {
            if (isOwnedByCurrentRegion(player)) {
                task.run();
                return;
            }
            player.getScheduler().execute(plugin, task, null, 0L);
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runGlobal(Runnable task) {
        if (folia) {
            if (Bukkit.getServer().isGlobalTickThread()) {
                task.run();
                return;
            }
            Bukkit.getGlobalRegionScheduler().execute(plugin, task);
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runAsync(Runnable task) {
        if (folia) {
            Bukkit.getAsyncScheduler().runNow(plugin, scheduled -> task.run());
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    public TaskHandle runRepeatingGlobal(Runnable task, long periodTicks) {
        return runRepeatingGlobal(task, periodTicks, periodTicks);
    }

    public TaskHandle runRepeatingGlobal(Runnable task, long initialDelayTicks, long periodTicks) {
        long delay = Math.max(1L, initialDelayTicks);
        long period = Math.max(1L, periodTicks);
        if (folia) {
            ScheduledTask scheduled = Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(plugin, st -> task.run(), delay, period);
            return new FoliaTaskHandle(scheduled);
        }
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        return new BukkitTaskHandle(bukkitTask);
    }

    public TaskHandle runRepeatingForPlayer(Player player, Consumer<Player> task, long periodTicks) {
        return runRepeatingForPlayer(player, task, periodTicks, periodTicks);
    }

    public TaskHandle runRepeatingForPlayer(
            Player player,
            Consumer<Player> task,
            long initialDelayTicks,
            long periodTicks
    ) {
        long delay = Math.max(1L, initialDelayTicks);
        long period = Math.max(1L, periodTicks);
        if (folia) {
            ScheduledTask scheduled = player.getScheduler().runAtFixedRate(
                    plugin,
                    st -> task.accept(player),
                    null,
                    delay,
                    period
            );
            return scheduled == null ? TaskHandle.NOOP : new FoliaTaskHandle(scheduled);
        }
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> {
                    if (player.isOnline()) {
                        task.accept(player);
                    }
                },
                delay,
                period
        );
        return new BukkitTaskHandle(bukkitTask);
    }

    private static boolean detectFolia() {
        try {
            return ServerBuildInfo.buildInfo().isBrandCompatible(FOLIA_BRAND);
        } catch (Throwable ignored) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                return true;
            } catch (ClassNotFoundException error) {
                return false;
            }
        }
    }

    private static boolean isOwnedByCurrentRegion(Entity entity) {
        try {
            return Bukkit.isOwnedByCurrentRegion(entity);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public interface TaskHandle {
        TaskHandle NOOP = () -> {
        };

        void cancel();
    }

    private static final class FoliaTaskHandle implements TaskHandle {
        private final ScheduledTask task;

        private FoliaTaskHandle(ScheduledTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }
    }

    private static final class BukkitTaskHandle implements TaskHandle {
        private final BukkitTask task;

        private BukkitTaskHandle(BukkitTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }
    }
}
