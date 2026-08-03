package net.opmasterleo.packetuxui.scheduler.bukkit;

import java.util.Objects;

import org.bukkit.scheduler.BukkitTask;

import net.opmasterleo.packetuxui.scheduler.TaskHandle;

final class BukkitTaskHandles {

    private BukkitTaskHandles() {
    }

    static TaskHandle of(BukkitTask task) {
        return of(task, false);
    }

    static TaskHandle of(BukkitTask task, boolean repeating) {
        return task == null ? TaskHandle.NOOP : new Handle(task, repeating);
    }

    private static final class Handle implements TaskHandle {
        private final BukkitTask task;
        private final boolean repeating;

        private Handle(BukkitTask task, boolean repeating) {
            this.task = Objects.requireNonNull(task, "task");
            this.repeating = repeating;
        }

        @Override
        public void cancel() {
            task.cancel();
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }

        @Override
        public boolean isRepeating() {
            return repeating;
        }
    }
}
