package net.opmasterleo.packetuxui.nms;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

/**
 * Vanilla-like 1..100 window id cycling for adapters that cannot call
 * {@code ServerPlayer.nextContainerCounter()}.
 */
public final class FallbackWindowIds {

    private static final ConcurrentMap<UUID, AtomicInteger> COUNTERS = new ConcurrentHashMap<>();
    private static final Function<UUID, AtomicInteger> COUNTER_FACTORY = new CounterFactory();
    private static final IntUnaryOperator ADVANCE = new WindowIdAdvance();

    private FallbackWindowIds() {
    }

    public static int next(UUID playerId) {
        return COUNTERS.computeIfAbsent(playerId, COUNTER_FACTORY).updateAndGet(ADVANCE);
    }

    public static void clear(UUID playerId) {
        COUNTERS.remove(playerId);
    }

    private static final class CounterFactory implements Function<UUID, AtomicInteger> {
        @Override
        public AtomicInteger apply(UUID id) {
            return new AtomicInteger(0);
        }
    }

    private static final class WindowIdAdvance implements IntUnaryOperator {
        @Override
        public int applyAsInt(int v) {
            return v >= 100 ? 1 : v + 1;
        }
    }
}
