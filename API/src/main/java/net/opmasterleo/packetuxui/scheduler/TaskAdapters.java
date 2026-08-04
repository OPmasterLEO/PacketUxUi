package net.opmasterleo.packetuxui.scheduler;

import java.util.Objects;
import java.util.function.Consumer;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Named Runnable/Consumer adapters — no lambdas on hot scheduler hops
 * (profiler frames stay readable: {@code TaskAdapters$ForPlayer} not {@code lambda$…}).
 */
public final class TaskAdapters {

    private TaskAdapters() {
    }

    public static Runnable forPlayer(Player player, Consumer<Player> task) {
        return new ForPlayer(player, task);
    }

    public static Runnable forPlayerOnline(Player player, Consumer<Player> task) {
        return new ForPlayerOnline(player, task);
    }

    public static Consumer<Entity> forPlayerEntity(Consumer<Player> task) {
        return new ForPlayerEntity(task);
    }

    public static Runnable acceptPlayer(Player player, Consumer<Player> task) {
        return new AcceptPlayer(player, task);
    }

    public static final class ForPlayer implements Runnable {
        private final Player player;
        private final Consumer<Player> task;

        public ForPlayer(Player player, Consumer<Player> task) {
            this.player = player;
            this.task = Objects.requireNonNull(task, "task");
        }

        @Override
        public void run() {
            task.accept(player);
        }
    }

    public static final class ForPlayerOnline implements Runnable {
        private final Player player;
        private final Consumer<Player> task;

        public ForPlayerOnline(Player player, Consumer<Player> task) {
            this.player = player;
            this.task = Objects.requireNonNull(task, "task");
        }

        @Override
        public void run() {
            if (player != null && player.isOnline()) {
                task.accept(player);
            }
        }
    }

    public static final class AcceptPlayer implements Runnable {
        private final Player player;
        private final Consumer<Player> task;

        public AcceptPlayer(Player player, Consumer<Player> task) {
            this.player = player;
            this.task = Objects.requireNonNull(task, "task");
        }

        @Override
        public void run() {
            task.accept(player);
        }
    }

    public static final class ForPlayerEntity implements Consumer<Entity> {
        private final Consumer<Player> task;

        public ForPlayerEntity(Consumer<Player> task) {
            this.task = Objects.requireNonNull(task, "task");
        }

        @Override
        public void accept(Entity entity) {
            if (entity instanceof Player online && online.isOnline()) {
                task.accept(online);
            }
        }
    }

    public static final class WrapRunnable implements Runnable {
        private final Runnable delegate;

        public WrapRunnable(Runnable delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public void run() {
            delegate.run();
        }
    }
}
