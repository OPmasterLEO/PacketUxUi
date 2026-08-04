package net.opmasterleo.packetuxui.scheduler;

/**
 * Platform-neutral scheduled task handle (Paper {@code ScheduledTask} / Bukkit {@code BukkitTask}).
 */
public interface TaskHandle {

    TaskHandle NOOP = new TaskHandle() {
        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return true;
        }

        @Override
        public boolean isRepeating() {
            return false;
        }

        @Override
        public State state() {
            return State.CANCELLED;
        }
    };

    enum State {
        IDLE,
        RUNNING,
        FINISHED,
        CANCELLED,
        /** Bukkit / unknown backends that do not expose execution state. */
        UNKNOWN
    }

    void cancel();

    boolean isCancelled();

    boolean isRepeating();

    default State state() {
        if (isCancelled()) {
            return State.CANCELLED;
        }
        return State.UNKNOWN;
    }
}
