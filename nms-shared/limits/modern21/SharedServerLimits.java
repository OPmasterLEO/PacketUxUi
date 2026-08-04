package net.opmasterleo.packetuxui.nms.shared;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.component.WritableBookContent;
import net.opmasterleo.packetuxui.nms.ServerLimits;

/**
 * Live vanilla limits from the running NMS jar (not copied magic numbers).
 */
public final class SharedServerLimits implements ServerLimits {

    private final MenuType<?>[] menuTypes;

    public SharedServerLimits() {
        this.menuTypes = new MenuType<?>[] {
                MenuType.GENERIC_9x1,
                MenuType.GENERIC_9x2,
                MenuType.GENERIC_9x3,
                MenuType.GENERIC_9x4,
                MenuType.GENERIC_9x5,
                MenuType.GENERIC_9x6,
                MenuType.GENERIC_3x3,
                MenuType.CRAFTER_3x3,
                MenuType.ANVIL,
                MenuType.BEACON,
                MenuType.BLAST_FURNACE,
                MenuType.BREWING_STAND,
                MenuType.CRAFTING,
                MenuType.ENCHANTMENT,
                MenuType.FURNACE,
                MenuType.GRINDSTONE,
                MenuType.HOPPER,
                MenuType.LECTERN,
                MenuType.LOOM,
                MenuType.MERCHANT,
                MenuType.SHULKER_BOX,
                MenuType.SMITHING,
                MenuType.SMOKER,
                MenuType.CARTOGRAPHY_TABLE,
                MenuType.STONECUTTER
        };
    }

    @Override
    public int bookMaxPages() {
        return WritableBookContent.MAX_PAGES;
    }

    @Override
    public int bookMaxPageLength() {
        return WritableBookContent.PAGE_EDIT_LENGTH;
    }

    @Override
    public int containerCounterMin() {
        return 1;
    }

    @Override
    public int containerCounterMax() {
        // Mirrors ServerPlayer.nextContainerCounter: value = value % 100 + 1
        int sample = 0;
        int max = 0;
        for (int i = 0; i < 256; i++) {
            sample = sample % 100 + 1;
            if (sample > max) {
                max = sample;
            }
            if (i > 0 && sample == 1) {
                return max;
            }
        }
        return max;
    }

    @Override
    public int playerInventorySlots() {
        return Inventory.INVENTORY_SIZE;
    }

    @Override
    public int hotbarSlots() {
        return Inventory.getSelectionSize();
    }

    @Override
    public int maxGenericChestRows() {
        int cols = hotbarSlots();
        int top = menuTypeTopSlots(5); // GENERIC_9x6 protocol id
        if (cols <= 0 || top <= 0) {
            return 6;
        }
        return Math.max(1, top / cols);
    }

    @Override
    public int menuTypeTopSlots(int windowTypeId) {
        if (windowTypeId < 0 || windowTypeId >= menuTypes.length) {
            return -1;
        }
        MenuType<?> type = menuTypes[windowTypeId];
        if (type == null) {
            return -1;
        }
        return topSlotsOf(type);
    }

    private int topSlotsOf(MenuType<?> type) {
        int cols = Inventory.getSelectionSize();
        if (type == MenuType.GENERIC_9x1) {
            return cols;
        }
        if (type == MenuType.GENERIC_9x2) {
            return cols * 2;
        }
        if (type == MenuType.GENERIC_9x3 || type == MenuType.SHULKER_BOX) {
            return cols * 3;
        }
        if (type == MenuType.GENERIC_9x4) {
            return cols * 4;
        }
        if (type == MenuType.GENERIC_9x5) {
            return cols * 5;
        }
        if (type == MenuType.GENERIC_9x6) {
            return cols * 6;
        }
        if (type == MenuType.GENERIC_3x3 || type == MenuType.CRAFTER_3x3) {
            return 3 * 3;
        }
        if (type == MenuType.HOPPER) {
            return HopperMenu.CONTAINER_SIZE;
        }
        if (type == MenuType.LECTERN) {
            return 1;
        }
        if (type == MenuType.BEACON) {
            return 1;
        }
        if (type == MenuType.ANVIL) {
            return 3;
        }
        if (type == MenuType.BLAST_FURNACE || type == MenuType.FURNACE || type == MenuType.SMOKER) {
            return 3;
        }
        if (type == MenuType.BREWING_STAND) {
            return 5;
        }
        if (type == MenuType.CRAFTING) {
            return 10;
        }
        if (type == MenuType.ENCHANTMENT) {
            return 2;
        }
        if (type == MenuType.GRINDSTONE) {
            return 3;
        }
        if (type == MenuType.LOOM) {
            return 4;
        }
        if (type == MenuType.MERCHANT) {
            return 3;
        }
        if (type == MenuType.SMITHING) {
            return 4;
        }
        if (type == MenuType.CARTOGRAPHY_TABLE) {
            return 3;
        }
        if (type == MenuType.STONECUTTER) {
            return 2;
        }
        return -1;
    }
}
