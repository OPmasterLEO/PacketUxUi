package net.opmasterleo.packetuxui.scheduler.paper;

import java.util.Objects;
import java.util.function.Consumer;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/** Named {@code Consumer<ScheduledTask>} wrappers — no lambdas in Paper scheduler adapters. */
final class PaperConsumers {

    private PaperConsumers() {
    }

    static Consumer<ScheduledTask> run(Runnable task) {
        return new Run(task);
    }

    static <T> Consumer<ScheduledTask> accept(T value, Consumer<? super T> task) {
        return new Accept<>(value, task);
    }

    private static final class Run implements Consumer<ScheduledTask> {
        private final Runnable task;

        private Run(Runnable task) {
            this.task = Objects.requireNonNull(task, "task");
        }

        @Override
        public void accept(ScheduledTask scheduledTask) {
            task.run();
        }
    }

    private static final class Accept<T> implements Consumer<ScheduledTask> {
        private final T value;
        private final Consumer<? super T> task;

        private Accept(T value, Consumer<? super T> task) {
            this.value = value;
            this.task = Objects.requireNonNull(task, "task");
        }

        @Override
        public void accept(ScheduledTask scheduledTask) {
            task.accept(value);
        }
    }
}
