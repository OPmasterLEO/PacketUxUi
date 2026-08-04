package net.opmasterleo.packetuxui.nms;

import org.bukkit.inventory.ItemStack;

import net.opmasterleo.packetuxui.nms.item.UxItem;

public interface ItemBridge {

    Object toNms(UxItem item);

    UxItem fromNms(Object nmsItem);

    ItemStack toBukkit(UxItem item);

    UxItem fromBukkit(ItemStack stack);

    UxItem empty();

    boolean isEmpty(UxItem item);

    /**
     * Warm conversion caches for items that will be sent soon (open/present).
     * No-op default for adapters without a cache.
     */
    default void preload(Iterable<UxItem> items) {
    }

    /** Drop conversion caches (reload / memory pressure). */
    default void clearCaches() {
    }
}
