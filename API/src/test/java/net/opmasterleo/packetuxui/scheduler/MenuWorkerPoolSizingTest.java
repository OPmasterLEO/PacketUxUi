package net.opmasterleo.packetuxui.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MenuWorkerPoolSizingTest {

    @Test
    void scalesWithCpuAndLeavesHeadroomOnLargeHosts() {
        assertEquals(1, MenuWorkerPool.Sizing.autoMaxWorkers(1));
        assertEquals(2, MenuWorkerPool.Sizing.autoMaxWorkers(2));
        assertEquals(4, MenuWorkerPool.Sizing.autoMaxWorkers(4));
        assertEquals(7, MenuWorkerPool.Sizing.autoMaxWorkers(8));
        assertEquals(12, MenuWorkerPool.Sizing.autoMaxWorkers(16));
        assertEquals(16, MenuWorkerPool.Sizing.autoMaxWorkers(32));
    }

    @Test
    void fromCpusKeepsCoreWithinMaxAndBoundedQueue() {
        MenuWorkerPool.Sizing sizing = MenuWorkerPool.Sizing.fromCpus(8);
        assertTrue(sizing.coreThreads >= 1);
        assertTrue(sizing.coreThreads <= sizing.maxThreads);
        assertEquals(7, sizing.maxThreads);
        assertTrue(sizing.queueCapacity >= sizing.maxThreads * 48);
    }

    @Test
    void fromCpusNeverExceedsHardCap() {
        MenuWorkerPool.Sizing sizing = MenuWorkerPool.Sizing.fromCpus(128);
        assertEquals(16, sizing.maxThreads);
        assertTrue(sizing.coreThreads <= 16);
    }
}
