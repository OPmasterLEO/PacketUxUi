package net.opmasterleo.packetuxui.nms.item;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;

/**
 * Menu item view. Field-based data is used for builder-created items and similarity.
 * When created from a real {@link ItemStack} ({@link #hasNativeBukkit()}), the cloned
 * Bukkit stack is preferred for packet conversion so enchantments, lore, NBT/DataComponents,
 * attributes, potions, etc. are preserved.
 */
public final class UxItem {

    public static final UxItem EMPTY = new UxItem(
            "minecraft:air",
            0,
            null,
            List.of(),
            Map.of(),
            true,
            null,
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
    /** Full Bukkit clone; excluded from equals/hashCode (cache keys stay field-based). */
    private final ItemStack nativeBukkit;
    private final boolean empty;

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
        this(
                materialKey,
                amount,
                name,
                lore,
                enchantments,
                hideEnchantments,
                customModelData,
                headTextureBase64,
                null
        );
    }

    public UxItem(
            String materialKey,
            int amount,
            Component name,
            List<Component> lore,
            Map<String, Integer> enchantments,
            boolean hideEnchantments,
            Integer customModelData,
            String headTextureBase64,
            ItemStack nativeBukkit
    ) {
        this.materialKey = materialKey == null ? "minecraft:air" : materialKey;
        this.amount = Math.max(0, amount);
        this.name = name;
        this.lore = lore == null ? List.of() : List.copyOf(lore);
        this.enchantments = enchantments == null ? Map.of() : Map.copyOf(enchantments);
        this.hideEnchantments = hideEnchantments;
        this.customModelData = customModelData;
        this.headTextureBase64 = headTextureBase64;
        this.nativeBukkit = nativeBukkit == null || nativeBukkit.getType().isAir()
                ? null
                : nativeBukkit.clone();
        this.empty = this.amount <= 0
                || this.materialKey.equals("minecraft:air")
                || this.materialKey.equals("air")
                || this.materialKey.endsWith(":air");
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

    /** True when this item carries a full Bukkit stack (NBT/components intact). */
    public boolean hasNativeBukkit() {
        return nativeBukkit != null;
    }

    /**
     * Cloned native Bukkit stack with {@link #amount()} applied, or {@code null}.
     */
    public ItemStack nativeBukkitClone() {
        if (nativeBukkit == null) {
            return null;
        }
        ItemStack clone = nativeBukkit.clone();
        int amt = Math.max(1, amount);
        if (clone.getAmount() != amt) {
            clone.setAmount(amt);
        }
        return clone;
    }

    public boolean isEmpty() {
        return empty;
    }

    public UxItem withAmount(int newAmount) {
        if (newAmount <= 0) {
            return EMPTY;
        }
        if (newAmount == amount) {
            return this;
        }
        ItemStack nativeCopy = null;
        if (nativeBukkit != null) {
            nativeCopy = nativeBukkit.clone();
            nativeCopy.setAmount(newAmount);
        }
        return new UxItem(
                materialKey,
                newAmount,
                name,
                lore,
                enchantments,
                hideEnchantments,
                customModelData,
                headTextureBase64,
                nativeCopy
        );
    }

    public boolean isSimilar(UxItem other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (empty && other.empty) {
            return true;
        }
        if (empty || other.empty) {
            return false;
        }
        // Prefer native identity when both carry full stacks (potions, etc.).
        if (nativeBukkit != null && other.nativeBukkit != null) {
            return nativeBukkit.isSimilar(other.nativeBukkit);
        }
        return hideEnchantments == other.hideEnchantments
                && materialKey.equals(other.materialKey)
                && Objects.equals(name, other.name)
                && lore.equals(other.lore)
                && enchantments.equals(other.enchantments)
                && Objects.equals(customModelData, other.customModelData)
                && Objects.equals(headTextureBase64, other.headTextureBase64);
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
        if (empty && other.empty) {
            return true;
        }
        if (nativeBukkit != null || other.nativeBukkit != null) {
            // Native-bearing items: amount + similar native (avoid cache collisions).
            return amount == other.amount && isSimilar(other);
        }
        return amount == other.amount && isSimilar(other);
    }

    @Override
    public int hashCode() {
        if (nativeBukkit != null) {
            int result = nativeBukkit.getType().hashCode();
            result = 31 * result + amount;
            ItemStack probe = nativeBukkit.clone();
            probe.setAmount(1);
            result = 31 * result + probe.hashCode();
            return result;
        }
        int result = materialKey.hashCode();
        result = 31 * result + amount;
        result = 31 * result + (name == null ? 0 : name.hashCode());
        result = 31 * result + lore.hashCode();
        result = 31 * result + enchantments.hashCode();
        result = 31 * result + Boolean.hashCode(hideEnchantments);
        result = 31 * result + (customModelData == null ? 0 : customModelData.hashCode());
        result = 31 * result + (headTextureBase64 == null ? 0 : headTextureBase64.hashCode());
        return result;
    }
}
