package net.opmasterleo.packetuxui.scheduler;

import java.util.function.Consumer;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public interface EntityTasks {

    void run(Entity entity, Runnable task, Runnable retired);

    TaskHandle runLater(Entity entity, Runnable task, Runnable retired, long delayTicks);

    TaskHandle runRepeating(
            Entity entity,
            Consumer<? super Entity> task,
            Runnable retired,
            long initialDelayTicks,
            long periodTicks
    );

    default TaskHandle runLaterForPlayer(Player player, Consumer<Player> task, long delayTicks) {
        return runLater(player, () -> {
            if (player != null && player.isOnline()) {
                task.accept(player);
            }
        }, null, delayTicks);
    }

    default TaskHandle runRepeatingForPlayer(
            Player player,
            Consumer<Player> task,
            long initialDelayTicks,
            long periodTicks
    ) {
        return runRepeating(player, entity -> {
            if (entity instanceof Player online && online.isOnline()) {
                task.accept(online);
            }
        }, null, initialDelayTicks, periodTicks);
    }

    static EntityTasks bukkit(JavaPlugin plugin) {
        return new net.opmasterleo.packetuxui.scheduler.bukkit.BukkitEntityTasks(plugin);
    }

    static EntityTasks paper(JavaPlugin plugin) {
        return new net.opmasterleo.packetuxui.scheduler.paper.PaperEntityTasks(plugin);
    }
}
