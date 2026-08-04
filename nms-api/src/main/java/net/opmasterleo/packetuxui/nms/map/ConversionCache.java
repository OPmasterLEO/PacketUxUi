package net.opmasterleo.packetuxui.nms.map;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Bounded lock-free conversion cache. Overflow evicts a random ~25% of keys
 * (no full wipe → no miss storm; no unbounded growth).
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
            trim();
        }
        return created;
    }

    /** Cached value without loading — null if absent. */
    public V peek(K key) {
        return key == null ? null : map.get(key);
    }

    public void put(K key, V value) {
        if (key == null || value == null) {
            return;
        }
        if (map.put(key, value) == null && approxSize.incrementAndGet() > maxEntries) {
            trim();
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

    private void trim() {
        int target = Math.max(16, maxEntries * 3 / 4);
        ArrayList<K> keys = new ArrayList<>(map.keySet());
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        while (approxSize.get() > target && !keys.isEmpty()) {
            int i = rng.nextInt(keys.size());
            K key = keys.remove(i);
            if (map.remove(key) != null) {
                approxSize.decrementAndGet();
            }
        }
        int actual = map.size();
        approxSize.set(actual);
    }
}
