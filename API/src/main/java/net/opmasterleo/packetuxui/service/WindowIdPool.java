package net.opmasterleo.packetuxui.service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.IntSupplier;

import org.bukkit.entity.Player;

import net.opmasterleo.packetuxui.nms.LiveLimits;

/**
 * Tracks which vanilla window id is currently owned by PacketUxUi per player.
 * Allocation itself comes from {@code MenuPacketBridge.allocateWindowId} (NMS counter).
 * Valid id range is {@link LiveLimits#containerCounterMin()}..{@link LiveLimits#containerCounterMax()}.
 */
public final class WindowIdPool {

    /**
     * @deprecated use {@link LiveLimits#containerCounterMin()}
     */
    @Deprecated
    public static final int MIN_ID = 1;
    /**
     * @deprecated use {@link LiveLimits#containerCounterMax()}
     */
    @Deprecated
    public static final int MAX_ID = 100;
    /** @deprecated No fixed virtual id; use per-player allocate. */
    @Deprecated
    public static final int LEGACY_FIXED_ID = 126;

    private final ConcurrentMap<UUID, Integer> assigned = new ConcurrentHashMap<>();

    /**
     * @deprecated Prefer {@link #isOurs(UUID, int)} — range is live NMS container-counter bounds.
     */
    @Deprecated
    public static boolean isVirtual(int windowId) {
        return isInCounterRange(windowId);
    }

    public static boolean isInCounterRange(int windowId) {
        return windowId >= LiveLimits.containerCounterMin()
                && windowId <= LiveLimits.containerCounterMax();
    }

    public int allocate(Player player, IntSupplier allocator) {
        return allocate(player.getUniqueId(), allocator);
    }

    public int allocate(UUID playerId, IntSupplier allocator) {
        Integer existing = assigned.get(playerId);
        if (existing != null) {
            return existing;
        }
        int id = allocator.getAsInt();
        assigned.put(playerId, id);
        return id;
    }

    /** Test helper: register a pre-chosen id without an allocator. */
    public int allocate(UUID playerId, int windowId) {
        Integer existing = assigned.get(playerId);
        if (existing != null) {
            return existing;
        }
        assigned.put(playerId, windowId);
        return windowId;
    }

    public int windowId(Player player) {
        return windowId(player.getUniqueId());
    }

    public int windowId(UUID playerId) {
        Integer id = assigned.get(playerId);
        return id != null ? id : -1;
    }

    public boolean isOurs(Player player, int windowId) {
        return isOurs(player.getUniqueId(), windowId);
    }

    public boolean isOurs(UUID playerId, int windowId) {
        Integer id = assigned.get(playerId);
        return id != null && id == windowId;
    }

    public void release(Player player) {
        release(player.getUniqueId());
    }

    public void release(UUID playerId) {
        assigned.remove(playerId);
    }
}
