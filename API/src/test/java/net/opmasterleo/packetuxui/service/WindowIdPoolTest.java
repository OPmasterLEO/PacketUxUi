package net.opmasterleo.packetuxui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class WindowIdPoolTest {

    @Test
    void allocatesStableIdPerUuid() {
        WindowIdPool pool = new WindowIdPool();
        UUID a = UUID.randomUUID();
        int first = pool.allocate(a);
        assertEquals(first, pool.allocate(a));
        assertEquals(first, pool.windowId(a));
    }

    @Test
    void releasesAndReuses() {
        WindowIdPool pool = new WindowIdPool();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        int idA = pool.allocate(a);
        int idB = pool.allocate(b);
        assertNotEquals(idA, idB);
        pool.release(a);
        assertEquals(-1, pool.windowId(a));
        UUID c = UUID.randomUUID();
        int idC = pool.allocate(c);
        assertEquals(idA, idC); // nextClearBit reclaims the lowest freed id
    }

    @Test
    void exhaustsPool() {
        WindowIdPool pool = new WindowIdPool();
        int span = WindowIdPool.MAX_ID - WindowIdPool.MIN_ID + 1;
        for (int i = 0; i < span; i++) {
            pool.allocate(UUID.randomUUID());
        }
        assertThrows(IllegalStateException.class, () -> pool.allocate(UUID.randomUUID()));
    }
}
