package net.opmasterleo.packetuxui.nms.map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Lock-free LRU-ish conversion cache: map once, reuse on the hot path.
 * Overflow clears the table (cheaper than synchronized LinkedHashMap eviction).
 *
 * @param <K> cache key (e.g. {@code UxItem})
 * @param <V> cached handle (e.g. NMS ItemStack prototype)
 */
public final class ConversionCache<K, V> {

    private final ConcurrentHashMap<K, V> map;
    private final AtomicInteger approxSize = new AtomicInteger();
    private final int maxEntries;
    private final Function<K, V> loader;

    public ConversionCache(int maxEntries, Function<K, V> loader) {
        if (maxEntries < 16) {
            throw new IllegalArgumentException("maxEntries");
        }
        this.maxEntries = maxEntries;
        this.loader = java.util.Objects.requireNonNull(loader, "loader");
        this.map = new ConcurrentHashMap<>(Math.min(256, maxEntries));
    }

    public V get(K key) {
        if (key == null) {
            return null;
        }
        V hit = map.get(key);
        if (hit != null) {
            return hit;
        }
        V created = loader.apply(key);
        if (created == null) {
            return null;
        }
        V raced = map.putIfAbsent(key, created);
        if (raced != null) {
            return raced;
        }
        if (approxSize.incrementAndGet() > maxEntries) {
            clear();
            map.put(key, created);
            approxSize.set(1);
        }
        return created;
    }

    public void put(K key, V value) {
        if (key == null || value == null) {
            return;
        }
        if (map.put(key, value) == null && approxSize.incrementAndGet() > maxEntries) {
            clear();
            map.put(key, value);
            approxSize.set(1);
        }
    }

    public void preload(Iterable<? extends K> keys) {
        if (keys == null) {
            return;
        }
        for (K key : keys) {
            if (key != null) {
                get(key);
            }
        }
    }

    public void clear() {
        map.clear();
        approxSize.set(0);
    }

    public int size() {
        return map.size();
    }
}
