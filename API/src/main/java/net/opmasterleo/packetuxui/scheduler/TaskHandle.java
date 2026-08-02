package net.opmasterleo.packetuxui.scheduler;

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
    };

    void cancel();

    boolean isCancelled();

    boolean isRepeating();
}
