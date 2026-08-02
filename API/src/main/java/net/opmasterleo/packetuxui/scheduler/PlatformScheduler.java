package net.opmasterleo.packetuxui.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlatformScheduler {

    private final JavaPlugin plugin;
    private final boolean folia;

    public PlatformScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
        boolean detected;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            detected = true;
        } catch (ClassNotFoundException ignored) {
            detected = false;
        }
        this.folia = detected;
    }

    public boolean isFolia() {
        return folia;
    }

    public void runForPlayer(Player player, Runnable task) {
        if (folia) {
            try {
                player.getScheduler().execute(plugin, task, null, 0L);
                return;
            } catch (Throwable ignored) {
            }
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runGlobal(Runnable task) {
        if (folia) {
            try {
                Bukkit.getGlobalRegionScheduler().execute(plugin, task);
                return;
            } catch (Throwable ignored) {
            }
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runRepeatingGlobal(Runnable task, long periodTicks) {
        if (folia) {
            try {
                Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduled -> task.run(), periodTicks, periodTicks);
                return;
            } catch (Throwable ignored) {
            }
        }
        Bukkit.getScheduler().runTaskTimer(plugin, task, periodTicks, periodTicks);
    }
}
