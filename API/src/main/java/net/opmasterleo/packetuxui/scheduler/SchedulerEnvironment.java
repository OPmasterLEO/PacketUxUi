package net.opmasterleo.packetuxui.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import io.papermc.paper.ServerBuildInfo;
import net.kyori.adventure.key.Key;

final class SchedulerEnvironment {

    private static final Key FOLIA_BRAND = Key.key("papermc", "folia");

    private final JavaPlugin plugin;
    private final boolean folia;
    private final boolean paperSchedulers;

    SchedulerEnvironment(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folia = detectFolia();
        this.paperSchedulers = detectPaperSchedulers();
    }

    JavaPlugin plugin() {
        return plugin;
    }

    boolean isFolia() {
        return folia;
    }

    boolean hasPaperSchedulers() {
        return paperSchedulers;
    }

    boolean isPrimaryThread() {
        return Bukkit.isPrimaryThread();
    }

    boolean isGlobalTickThread() {
        if (!paperSchedulers) {
            return Bukkit.isPrimaryThread();
        }
        try {
            return Bukkit.getServer().isGlobalTickThread();
        } catch (Throwable ignored) {
            return Bukkit.isPrimaryThread();
        }
    }

    boolean isOwnedByCurrentRegion(Entity entity) {
        if (!paperSchedulers || entity == null) {
            return Bukkit.isPrimaryThread();
        }
        try {
            return Bukkit.isOwnedByCurrentRegion(entity);
        } catch (Throwable ignored) {
            return Bukkit.isPrimaryThread();
        }
    }

    boolean isOwnedByCurrentRegion(Location location) {
        if (!paperSchedulers || location == null || location.getWorld() == null) {
            return Bukkit.isPrimaryThread();
        }
        try {
            return Bukkit.isOwnedByCurrentRegion(location);
        } catch (Throwable ignored) {
            return Bukkit.isPrimaryThread();
        }
    }

    boolean isOwnedByCurrentRegion(Block block) {
        if (!paperSchedulers || block == null) {
            return Bukkit.isPrimaryThread();
        }
        try {
            return Bukkit.isOwnedByCurrentRegion(block);
        } catch (Throwable ignored) {
            return Bukkit.isPrimaryThread();
        }
    }

    boolean isOwnedByCurrentRegion(World world, int chunkX, int chunkZ) {
        if (!paperSchedulers || world == null) {
            return Bukkit.isPrimaryThread();
        }
        try {
            return Bukkit.isOwnedByCurrentRegion(world, chunkX, chunkZ);
        } catch (Throwable ignored) {
            return Bukkit.isPrimaryThread();
        }
    }

    static long ticks(long ticks) {
        return Math.max(1L, ticks);
    }

    void cancelAll() {
        if (paperSchedulers) {
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

    private static boolean detectPaperSchedulers() {
        try {
            Bukkit.class.getMethod("getRegionScheduler");
            Bukkit.class.getMethod("getGlobalRegionScheduler");
            Bukkit.class.getMethod("getAsyncScheduler");
            return true;
        } catch (NoSuchMethodException | NoClassDefFoundError error) {
            return false;
        }
    }
}
