package net.opmasterleo.packetuxui.types;

/**
 * Named top-slot indices for non-chest (and shulker) layouts.
 * Use with {@link net.opmasterleo.packetuxui.manager.MenuBuild#type(InventoryType)}.
 */
public final class InventorySlots {

    private InventorySlots() {
    }

    public static final int HOPPER_0 = 0;
    public static final int HOPPER_1 = 1;
    public static final int HOPPER_2 = 2;
    public static final int HOPPER_3 = 3;
    public static final int HOPPER_4 = 4;
    /** Center of the 5-slot hopper row. */
    public static final int HOPPER_CENTER = HOPPER_2;

    public static final int ANVIL_LEFT = 0;
    public static final int ANVIL_RIGHT = 1;
    public static final int ANVIL_RESULT = 2;

    public static final int FURNACE_INGREDIENT = 0;
    public static final int FURNACE_FUEL = 1;
    public static final int FURNACE_RESULT = 2;

    public static final int BREWING_BOTTLE_0 = 0;
    public static final int BREWING_BOTTLE_1 = 1;
    public static final int BREWING_BOTTLE_2 = 2;
    public static final int BREWING_INGREDIENT = 3;
    public static final int BREWING_FUEL = 4;

    public static final int CRAFTING_RESULT = 0;
    public static final int CRAFTING_GRID_START = 1;
    public static final int CRAFTING_GRID_END = 9;

    public static final int ENCHANT_ITEM = 0;
    public static final int ENCHANT_LAPIS = 1;

    public static final int GRINDSTONE_INPUT_0 = 0;
    public static final int GRINDSTONE_INPUT_1 = 1;
    public static final int GRINDSTONE_RESULT = 2;

    public static final int LOOM_BANNER = 0;
    public static final int LOOM_DYE = 1;
    public static final int LOOM_PATTERN = 2;
    public static final int LOOM_RESULT = 3;

    public static final int MERCHANT_INPUT_0 = 0;
    public static final int MERCHANT_INPUT_1 = 1;
    public static final int MERCHANT_RESULT = 2;

    public static final int SMITHING_TEMPLATE = 0;
    public static final int SMITHING_BASE = 1;
    public static final int SMITHING_ADDITION = 2;
    public static final int SMITHING_RESULT = 3;

    public static final int CARTOGRAPHY_MAP = 0;
    public static final int CARTOGRAPHY_PAPER = 1;
    public static final int CARTOGRAPHY_RESULT = 2;

    public static final int STONECUTTER_INPUT = 0;
    public static final int STONECUTTER_RESULT = 1;

    public static final int BEACON_PAYMENT = 0;
    public static final int LECTERN_BOOK = 0;

    public static final int DISPENSER_GRID = 9;
}
