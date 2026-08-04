package net.opmasterleo.packetuxui.nms;

/**
 * Vanilla server limits read from NMS (not hardcoded copies).
 * Call sites should use these accessors so values track the running server.
 */
public interface ServerLimits {

    /** Fallback when no adapter is bound (unit tests / pre-init). */
    ServerLimits FALLBACK = new Fallback();

    /** {@code WritableBookContent.MAX_PAGES} (written/writable book page cap). */
    int bookMaxPages();

    /** {@code WritableBookContent.PAGE_EDIT_LENGTH} (max characters per page). */
    int bookMaxPageLength();

    /** Lowest container sync id from {@code ServerPlayer.nextContainerCounter()} cycle. */
    int containerCounterMin();

    /** Highest container sync id in the vanilla counter cycle. */
    int containerCounterMax();

    /** {@code Inventory.INVENTORY_SIZE} — storage + hotbar slots in Set Content. */
    int playerInventorySlots();

    /** Hotbar selection size ({@code Inventory.getSelectionSize()}). */
    int hotbarSlots();

    /** Player main storage rows × columns (inventory size minus hotbar). */
    default int playerStorageSlots() {
        return Math.max(0, playerInventorySlots() - hotbarSlots());
    }

    /** Max generic chest rows (GENERIC_9x1 … GENERIC_9x6). */
    int maxGenericChestRows();

    /**
     * Protocol top-slot count for an Open Screen type id, or {@code -1} if unknown
     * (caller keeps enum fallback).
     */
    int menuTypeTopSlots(int windowTypeId);

    final class Fallback implements ServerLimits {
        @Override
        public int bookMaxPages() {
            return 100;
        }

        @Override
        public int bookMaxPageLength() {
            return 1024;
        }

        @Override
        public int containerCounterMin() {
            return 1;
        }

        @Override
        public int containerCounterMax() {
            return 100;
        }

        @Override
        public int playerInventorySlots() {
            return 36;
        }

        @Override
        public int hotbarSlots() {
            return 9;
        }

        @Override
        public int maxGenericChestRows() {
            return 6;
        }

        @Override
        public int menuTypeTopSlots(int windowTypeId) {
            return -1;
        }
    }
}
