package net.opmasterleo.packetuxui.scheduler;

import org.bukkit.plugin.java.JavaPlugin;

public interface GlobalTasks {

    void run(Runnable task);

    TaskHandle runNextTick(Runnable task);

    TaskHandle runLater(Runnable task, long delayTicks);

    TaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks);

    static GlobalTasks bukkit(JavaPlugin plugin) {
        return new net.opmasterleo.packetuxui.scheduler.bukkit.BukkitGlobalTasks(plugin);
    }

    static GlobalTasks paper(JavaPlugin plugin) {
        return new net.opmasterleo.packetuxui.scheduler.paper.PaperGlobalTasks(plugin);
    }
}
