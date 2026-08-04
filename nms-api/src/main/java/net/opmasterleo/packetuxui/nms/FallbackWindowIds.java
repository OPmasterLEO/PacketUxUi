package net.opmasterleo.packetuxui.nms;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Vanilla-like 1..100 window id cycling for adapters that cannot call
 * {@code ServerPlayer.nextContainerCounter()}.
 */
public final class FallbackWindowIds {

    private static final ConcurrentMap<UUID, AtomicInteger> COUNTERS = new ConcurrentHashMap<>();

    private FallbackWindowIds() {
    }

    public static int next(UUID playerId) {
        return COUNTERS.computeIfAbsent(playerId, id -> new AtomicInteger(0))
                .updateAndGet(v -> v >= 100 ? 1 : v + 1);
    }

    public static void clear(UUID playerId) {
        COUNTERS.remove(playerId);
    }
}
