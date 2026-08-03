package net.opmasterleo.packetuxui.types;

public enum InventoryType {
    GENERIC9X1(9, 0),
    GENERIC9X2(18, 1),
    GENERIC9X3(27, 2),
    GENERIC9X4(36, 3),
    GENERIC9X5(45, 4),
    GENERIC9X6(54, 5),
    GENERIC3X3(9, 6),
    CRAFTER3X3(10, 7),
    ANVIL(3, 8),
    BEACON(1, 9),
    BLAST_FURNACE(3, 10),
    BREWING_STAND(4, 11),
    CRAFTING_TABLE(10, 12),
    ENCHANTMENT_TABLE(2, 13),
    FURNACE(3, 14),
    GRINDSTONE(3, 15),
    HOPPER(5, 16),
    LECTERN(0, 17),
    LOOM(4, 18),
    VILLAGER(3, 19),
    SHULKER_BOX(27, 20),
    SMITHING_TABLE(4, 21),
    SMOKER(3, 22),
    CARTOGRAPHY_TABLE(3, 23),
    STONECUTTER(2, 24),
    CUSTOM9x7(63, 5),
    CUSTOM9x8(72, 5),
    CUSTOM9x9(81, 5),
    CUSTOM9x10(90, 5);

    private final int size;
    private final int windowTypeId;

    InventoryType(int slots, int windowTypeId) {
        this.size = slots;
        this.windowTypeId = windowTypeId;
    }

    public int size() {
        return size;
    }

    public int protocolTopSize() {
        return switch (this) {
            case CUSTOM9x7, CUSTOM9x8, CUSTOM9x9, CUSTOM9x10 -> 54;
            default -> size;
        };
    }

    public int lastIndex() {
        return size - 1;
    }

    public int protocolLastIndex() {
        return protocolTopSize() - 1;
    }

    public int id() {
        return windowTypeId;
    }
}
