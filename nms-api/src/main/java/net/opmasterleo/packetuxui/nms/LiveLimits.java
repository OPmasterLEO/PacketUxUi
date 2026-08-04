package net.opmasterleo.packetuxui.nms;

/**
 * Process-wide live limits from the loaded {@link NmsAdapter}.
 * Always safe before init (returns {@link ServerLimits#FALLBACK}).
 */
public final class LiveLimits {

    private static volatile ServerLimits active = ServerLimits.FALLBACK;

    private LiveLimits() {
    }

    public static void bind(ServerLimits limits) {
        active = limits == null ? ServerLimits.FALLBACK : limits;
    }

    public static void unbind() {
        active = ServerLimits.FALLBACK;
    }

    public static ServerLimits get() {
        return active;
    }

    public static int bookMaxPages() {
        return active.bookMaxPages();
    }

    public static int bookMaxPageLength() {
        return active.bookMaxPageLength();
    }

    public static int containerCounterMin() {
        return active.containerCounterMin();
    }

    public static int containerCounterMax() {
        return active.containerCounterMax();
    }

    public static int playerInventorySlots() {
        return active.playerInventorySlots();
    }

    public static int hotbarSlots() {
        return active.hotbarSlots();
    }

    public static int playerStorageSlots() {
        return active.playerStorageSlots();
    }

    public static int maxGenericChestRows() {
        return active.maxGenericChestRows();
    }

    public static int menuTypeTopSlots(int windowTypeId) {
        return active.menuTypeTopSlots(windowTypeId);
    }
}
