package net.opmasterleo.packetuxui.scheduler;

import org.bukkit.scheduler.BukkitTask;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

final class TaskHandles {

    private TaskHandles() {
    }

    static TaskHandle of(ScheduledTask task) {
        return task == null ? TaskHandle.NOOP : new PaperHandle(task);
    }

    static TaskHandle of(BukkitTask task) {
        return of(task, false);
    }

    static TaskHandle of(BukkitTask task, boolean repeating) {
        return task == null ? TaskHandle.NOOP : new BukkitHandle(task, repeating);
    }

    private static final class PaperHandle implements TaskHandle {
        private final ScheduledTask task;

        private PaperHandle(ScheduledTask task) {
            this.task = task;
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
            return task.isRepeatingTask();
        }
    }

    private static final class BukkitHandle implements TaskHandle {
        private final BukkitTask task;
        private final boolean repeating;

        private BukkitHandle(BukkitTask task, boolean repeating) {
            this.task = task;
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
