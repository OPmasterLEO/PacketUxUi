package net.opmasterleo.packetuxui.service;

import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.entity.Player;

public final class WindowIdPool {

    public static final int MIN_ID = 100;
    public static final int MAX_ID = 126;
    @Deprecated
    public static final int LEGACY_FIXED_ID = 126;

    private final ConcurrentMap<Player, Integer> assigned = new ConcurrentHashMap<>();
    private final BitSet inUse = new BitSet(MAX_ID + 1);
    private int cursor = MIN_ID;

    public synchronized int allocate(Player player) {
        Integer existing = assigned.get(player);
        if (existing != null) {
            return existing;
        }
        int id = nextFree();
        inUse.set(id);
        assigned.put(player, id);
        return id;
    }

    public int windowId(Player player) {
        Integer id = assigned.get(player);
        return id != null ? id : -1;
    }

    public synchronized void release(Player player) {
        Integer id = assigned.remove(player);
        if (id != null) {
            inUse.clear(id);
        }
    }

    private int nextFree() {
        int span = MAX_ID - MIN_ID + 1;
        for (int i = 0; i < span; i++) {
            int candidate = cursor;
            cursor = candidate >= MAX_ID ? MIN_ID : candidate + 1;
            if (!inUse.get(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Virtual window id pool exhausted (" + MIN_ID + ".." + MAX_ID + ")");
    }
}
