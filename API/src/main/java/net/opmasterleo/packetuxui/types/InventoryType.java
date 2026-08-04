package net.opmasterleo.packetuxui.types;

/**
 * Vanilla Open Screen window types (protocol ids 0–24) plus oversized CUSTOM chests
 * that open as {@link #GENERIC9X6}.
 * <p>
 * {@link #size()} is the contiguous top-slot count used for buttons / SetContent tops.
 * {@link #bottomSlotCount()} is usually 36 (player storage + hotbar); {@link #LECTERN} is 0.
 * <p>
 * {@link #supportsChestBind()} is true only for generic 9×N (and CUSTOM* as 9×6) — those bind
 * a real NMS {@code ChestMenu}. Hopper, anvil, furnace, etc. stay <b>packet-only</b>
 * (correct Open Screen type + contents; no wrong-size chest bind).
 */
public enum InventoryType {
    GENERIC9X1(9, 0, 36),
    GENERIC9X2(18, 1, 36),
    GENERIC9X3(27, 2, 36),
    GENERIC9X4(36, 3, 36),
    GENERIC9X5(45, 4, 36),
    GENERIC9X6(54, 5, 36),
    /** Dispenser / dropper layout (3×3). */
    GENERIC3X3(9, 6, 36),
    /** Crafter grid (0–8); player inv follows. Packet-only. */
    CRAFTER3X3(9, 7, 36),
    /** 0 input left, 1 input right, 2 result. Packet-only (+ rename packet optional). */
    ANVIL(3, 8, 36),
    /** 0 payment. Packet-only. */
    BEACON(1, 9, 36),
    /** 0 ingredient, 1 fuel, 2 result. Packet-only. */
    BLAST_FURNACE(3, 10, 36),
    /** 0–2 bottles, 3 ingredient, 4 blaze powder. Packet-only. */
    BREWING_STAND(5, 11, 36),
    /** 0 result, 1–9 grid. Packet-only. */
    CRAFTING_TABLE(10, 12, 36),
    /** 0 item, 1 lapis. Packet-only. */
    ENCHANTMENT_TABLE(2, 13, 36),
    /** 0 ingredient, 1 fuel, 2 result. Packet-only. */
    FURNACE(3, 14, 36),
    /** 0–1 inputs, 2 result. Packet-only. */
    GRINDSTONE(3, 15, 36),
    /** 0–4 row. Packet-only (do not fake as GENERIC9X1). */
    HOPPER(5, 16, 36),
    /** Single book slot; no player inventory in the container. Packet-only. */
    LECTERN(1, 17, 0),
    /** 0 banner, 1 dye, 2 pattern, 3 result. Packet-only. */
    LOOM(4, 18, 36),
    /** Merchant / villager: 0–1 inputs, 2 result. Packet-only. */
    VILLAGER(3, 19, 36),
    /** Same 27-slot map as GENERIC9X3; shulker texture. Packet-only bind (not ChestMenu). */
    SHULKER_BOX(27, 20, 36),
    /** 0 template, 1 base, 2 addition, 3 result. Packet-only. */
    SMITHING_TABLE(4, 21, 36),
    /** 0 ingredient, 1 fuel, 2 result. Packet-only. */
    SMOKER(3, 22, 36),
    /** 0 map, 1 paper, 2 result. Packet-only. */
    CARTOGRAPHY_TABLE(3, 23, 36),
    /** 0 input, 1 result. Packet-only. */
    STONECUTTER(2, 24, 36),
    CUSTOM9x7(63, 5, 36),
    CUSTOM9x8(72, 5, 36),
    CUSTOM9x9(81, 5, 36),
    CUSTOM9x10(90, 5, 36);

    private final int size;
    private final int windowTypeId;
    private final int bottomSlotCount;

    InventoryType(int slots, int windowTypeId, int bottomSlotCount) {
        this.size = slots;
        this.windowTypeId = windowTypeId;
        this.bottomSlotCount = bottomSlotCount;
    }

    /** Contiguous virtual top slots (button indices / item list length). */
    public int size() {
        return size;
    }

    /**
     * Top slots included in Set Content. CUSTOM* open as GENERIC_9x6 so protocol top is 54.
     */
    public int protocolTopSize() {
        return switch (this) {
            case CUSTOM9x7, CUSTOM9x8, CUSTOM9x9, CUSTOM9x10 -> 54;
            default -> size;
        };
    }

    /**
     * Player inventory slots appended after the top in Set Content (36 = storage+hotbar).
     * {@link #LECTERN} is 0 — the container has no player inv section.
     */
    public int bottomSlotCount() {
        return bottomSlotCount;
    }

    /** {@link #protocolTopSize()} + {@link #bottomSlotCount()}. */
    public int totalProtocolSlots() {
        return protocolTopSize() + bottomSlotCount;
    }

    public int lastIndex() {
        return size - 1;
    }

    public int protocolLastIndex() {
        return protocolTopSize() - 1;
    }

    /** Protocol window type id for Open Screen. */
    public int id() {
        return windowTypeId;
    }

    public boolean isGenericChest() {
        return switch (this) {
            case GENERIC9X1, GENERIC9X2, GENERIC9X3, GENERIC9X4, GENERIC9X5, GENERIC9X6,
                    CUSTOM9x7, CUSTOM9x8, CUSTOM9x9, CUSTOM9x10 -> true;
            default -> false;
        };
    }

    /** Chest rows 1–6 for generic types; {@code -1} for non-chest layouts. */
    public int chestRows() {
        return switch (this) {
            case GENERIC9X1 -> 1;
            case GENERIC9X2 -> 2;
            case GENERIC9X3 -> 3;
            case GENERIC9X4 -> 4;
            case GENERIC9X5 -> 5;
            case GENERIC9X6, CUSTOM9x7, CUSTOM9x8, CUSTOM9x9, CUSTOM9x10 -> 6;
            case SHULKER_BOX -> 3;
            default -> -1;
        };
    }

    public static InventoryType genericRows(int rows) {
        return switch (rows) {
            case 1 -> GENERIC9X1;
            case 2 -> GENERIC9X2;
            case 3 -> GENERIC9X3;
            case 4 -> GENERIC9X4;
            case 5 -> GENERIC9X5;
            case 6 -> GENERIC9X6;
            default -> throw new IllegalArgumentException("rows must be 1..6, got " + rows);
        };
    }

    /**
     * True for generic 9×N (and CUSTOM* that open as GENERIC_9x6). Only these bind a real
     * NMS {@code ChestMenu}. All other types are packet-only Open Screen + contents.
     */
    public boolean supportsChestBind() {
        return windowTypeId >= 0 && windowTypeId <= 5;
    }

    /** Alias of {@link #supportsChestBind()} — server container bind is chest-shaped only. */
    public boolean supportsServerBind() {
        return supportsChestBind();
    }
}
