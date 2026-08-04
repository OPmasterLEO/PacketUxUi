package net.opmasterleo.packetuxui.scheduler;

import java.util.function.Consumer;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entity-thread tasks — Folia/Paper {@code EntityScheduler}, Bukkit main thread.
 * Use for all player inventory / virtual-menu mutations.
 */
public interface EntityTasks {

    /**
     * Run ASAP on the entity's region (inline if already owned).
     * Folia: {@code EntityScheduler.execute(..., delay=0)}.
     */
    void run(Entity entity, Runnable task, Runnable retired);

    /**
     * Schedule for the entity's next tick and return a cancellable handle.
     * Folia: {@code EntityScheduler.run(plugin, task, retired)}.
     */
    TaskHandle runNextTick(Entity entity, Runnable task, Runnable retired);

    TaskHandle runLater(Entity entity, Runnable task, Runnable retired, long delayTicks);

    TaskHandle runRepeating(
            Entity entity,
            Consumer<? super Entity> task,
            Runnable retired,
            long initialDelayTicks,
            long periodTicks
    );

    default void run(Entity entity, Runnable task) {
        run(entity, task, null);
    }

    default TaskHandle runNextTick(Entity entity, Runnable task) {
        return runNextTick(entity, task, null);
    }

    default TaskHandle runLater(Entity entity, Runnable task, long delayTicks) {
        return runLater(entity, task, null, delayTicks);
    }

    default TaskHandle runLaterForPlayer(Player player, Consumer<Player> task, long delayTicks) {
        return runLater(player, () -> {
            if (player != null && player.isOnline()) {
                task.accept(player);
            }
        }, null, delayTicks);
    }

    default TaskHandle runNextTickForPlayer(Player player, Consumer<Player> task) {
        return runNextTick(player, () -> {
            if (player != null && player.isOnline()) {
                task.accept(player);
            }
        }, null);
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
