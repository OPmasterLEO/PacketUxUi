package net.opmasterleo.packetuxui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class WindowIdPoolTest {

    @Test
    void allocatesStableIdPerUuid() {
        WindowIdPool pool = new WindowIdPool();
        UUID a = UUID.randomUUID();
        AtomicInteger counter = new AtomicInteger(0);
        int first = pool.allocate(a, () -> counter.incrementAndGet());
        assertEquals(1, first);
        assertEquals(first, pool.allocate(a, () -> counter.incrementAndGet()));
        assertEquals(first, pool.windowId(a));
        assertEquals(1, counter.get()); // allocator not called again while held
    }

    @Test
    void isOursTracksAssignedOnly() {
        WindowIdPool pool = new WindowIdPool();
        UUID a = UUID.randomUUID();
        int id = pool.allocate(a, 42);
        assertTrue(pool.isOurs(a, 42));
        assertFalse(pool.isOurs(a, 43));
        assertFalse(pool.isOurs(UUID.randomUUID(), 42));
        pool.release(a);
        assertFalse(pool.isOurs(a, 42));
        assertEquals(-1, pool.windowId(a));
    }

    @Test
    void releaseAllowsNewVanillaId() {
        WindowIdPool pool = new WindowIdPool();
        UUID a = UUID.randomUUID();
        int first = pool.allocate(a, 7);
        pool.release(a);
        int second = pool.allocate(a, 8);
        assertEquals(7, first);
        assertEquals(8, second);
        assertNotEquals(first, second);
    }

    @Test
    void doesNotUseStickyMagicHundred() {
        WindowIdPool pool = new WindowIdPool();
        UUID a = UUID.randomUUID();
        AtomicInteger vanilla = new AtomicInteger(0);
        int id = pool.allocate(a, () -> {
            int next = vanilla.updateAndGet(v -> v >= 100 ? 1 : v + 1);
            return next;
        });
        assertTrue(id >= 1 && id <= 100);
        assertNotEquals(126, id);
        assertFalse(id >= 101 && id <= 126);
    }
}
