package net.opmasterleo.packetuxui.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SchedulerKindTest {

    @Test
    void kindsAreDistinct() {
        assertEquals(2, SchedulerKind.values().length);
        assertTrue(SchedulerKind.PAPER.name().contains("PAPER"));
        assertTrue(SchedulerKind.BUKKIT.name().contains("BUKKIT"));
        assertFalse(SchedulerKind.BUKKIT == SchedulerKind.PAPER);
    }

    @Test
    void ticksFloorAtOne() {
        assertEquals(1L, ServerPlatform.periodTicks(0));
        assertEquals(1L, ServerPlatform.periodTicks(-5));
        assertEquals(3L, ServerPlatform.periodTicks(3));
        assertEquals(0L, ServerPlatform.delayTicks(0));
        assertEquals(0L, ServerPlatform.delayTicks(-5));
        assertEquals(3L, ServerPlatform.delayTicks(3));
    }
}
