package net.opmasterleo.packetuxui.scheduler.paper;

import java.util.Objects;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.opmasterleo.packetuxui.scheduler.TaskHandle;

final class PaperTaskHandles {

    private PaperTaskHandles() {
    }

    static TaskHandle of(ScheduledTask task) {
        return task == null ? TaskHandle.NOOP : new Handle(task);
    }

    private static final class Handle implements TaskHandle {
        private final ScheduledTask task;

        private Handle(ScheduledTask task) {
            this.task = Objects.requireNonNull(task, "task");
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
}
