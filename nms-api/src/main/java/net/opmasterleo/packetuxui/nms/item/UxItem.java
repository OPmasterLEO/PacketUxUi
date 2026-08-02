package net.opmasterleo.packetuxui.nms.item;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.kyori.adventure.text.Component;

public final class UxItem {

    public static final UxItem EMPTY = new UxItem(
            "minecraft:air",
            0,
            null,
            List.of(),
            Map.of(),
            true,
            null,
            null
    );

    private final String materialKey;
    private final int amount;
    private final Component name;
    private final List<Component> lore;
    private final Map<String, Integer> enchantments;
    private final boolean hideEnchantments;
    private final Integer customModelData;
    private final String headTextureBase64;

    public UxItem(
            String materialKey,
            int amount,
            Component name,
            List<Component> lore,
            Map<String, Integer> enchantments,
            boolean hideEnchantments,
            Integer customModelData,
            String headTextureBase64
    ) {
        this.materialKey = materialKey == null ? "minecraft:air" : materialKey;
        this.amount = Math.max(0, amount);
        this.name = name;
        this.lore = lore == null ? List.of() : List.copyOf(lore);
        this.enchantments = enchantments == null ? Map.of() : Map.copyOf(enchantments);
        this.hideEnchantments = hideEnchantments;
        this.customModelData = customModelData;
        this.headTextureBase64 = headTextureBase64;
    }

    public String materialKey() {
        return materialKey;
    }

    public int amount() {
        return amount;
    }

    public Component name() {
        return name;
    }

    public List<Component> lore() {
        return lore;
    }

    public Map<String, Integer> enchantments() {
        return enchantments;
    }

    public boolean hideEnchantments() {
        return hideEnchantments;
    }

    public Integer customModelData() {
        return customModelData;
    }

    public String headTextureBase64() {
        return headTextureBase64;
    }

    public boolean isEmpty() {
        return amount <= 0 || materialKey.endsWith(":air") || materialKey.equals("air");
    }

    public static UxItemBuilder builder() {
        return new UxItemBuilder();
    }

    public static UxItemBuilder builder(String materialKey) {
        return new UxItemBuilder().material(materialKey);
    }

    public static UxHeadItemBuilder skull() {
        return new UxHeadItemBuilder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UxItem other)) {
            return false;
        }
        return amount == other.amount
                && hideEnchantments == other.hideEnchantments
                && Objects.equals(materialKey, other.materialKey)
                && Objects.equals(name, other.name)
                && Objects.equals(lore, other.lore)
                && Objects.equals(enchantments, other.enchantments)
                && Objects.equals(customModelData, other.customModelData)
                && Objects.equals(headTextureBase64, other.headTextureBase64);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                materialKey,
                amount,
                name,
                lore,
                enchantments,
                hideEnchantments,
                customModelData,
                headTextureBase64
        );
    }
}
