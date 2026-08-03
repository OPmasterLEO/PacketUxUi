package net.opmasterleo.packetuxui.service;

import java.util.BitSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.entity.Player;

public final class WindowIdPool {

    public static final int MIN_ID = 100;
    public static final int MAX_ID = 126;
    @Deprecated
    public static final int LEGACY_FIXED_ID = 126;

    private final ConcurrentMap<UUID, Integer> assigned = new ConcurrentHashMap<>();
    private final BitSet inUse = new BitSet(MAX_ID + 1);

    public synchronized int allocate(Player player) {
        return allocate(player.getUniqueId());
    }

    public synchronized int allocate(UUID playerId) {
        Integer existing = assigned.get(playerId);
        if (existing != null) {
            return existing;
        }
        int id = nextFree();
        inUse.set(id);
        assigned.put(playerId, id);
        return id;
    }

    public int windowId(Player player) {
        return windowId(player.getUniqueId());
    }

    public int windowId(UUID playerId) {
        Integer id = assigned.get(playerId);
        return id != null ? id : -1;
    }

    public synchronized void release(Player player) {
        release(player.getUniqueId());
    }

    public synchronized void release(UUID playerId) {
        Integer id = assigned.remove(playerId);
        if (id != null) {
            inUse.clear(id);
        }
    }

    private int nextFree() {
        int id = inUse.nextClearBit(MIN_ID);
        if (id > MAX_ID) {
            throw new IllegalStateException("Virtual window id pool exhausted (" + MIN_ID + ".." + MAX_ID + ")");
        }
        return id;
    }
}
