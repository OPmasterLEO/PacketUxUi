package net.opmasterleo.packetuxui.nms.shared;

import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import net.kyori.adventure.text.Component;
import net.minecraft.world.item.ItemStack;
import net.opmasterleo.packetuxui.nms.ItemBridge;
import net.opmasterleo.packetuxui.nms.item.SkullTextures;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.nms.map.BukkitKeyMaps;
import net.opmasterleo.packetuxui.nms.map.ConversionCache;

/**
 * Item conversion with runtime mapping caches. Native Bukkit stacks (from
 * {@link #fromBukkit}) are sent as-is so enchantments, lore, NBT/DataComponents,
 * attributes, potions, etc. round-trip. Builder-only UxItems still use the field path.
 */
public final class SharedItemBridge implements ItemBridge {

    private final ConversionCache<UxItem, ItemStack> nmsCache;
    private final ConversionCache<UxItem, org.bukkit.inventory.ItemStack> bukkitCache;

    public SharedItemBridge() {
        this.nmsCache = new ConversionCache<>(1024, new ToMinecraft());
        this.bukkitCache = new ConversionCache<>(1024, new ToBukkit());
    }

    private final class ToMinecraft implements java.util.function.Function<UxItem, ItemStack> {
        @Override
        public ItemStack apply(UxItem item) {
            return buildMinecraft(item);
        }
    }

    private final class ToBukkit implements java.util.function.Function<UxItem, org.bukkit.inventory.ItemStack> {
        @Override
        public org.bukkit.inventory.ItemStack apply(UxItem item) {
            return buildBukkit(item);
        }
    }

    @Override
    public Object toNms(UxItem item) {
        return toMinecraft(item);
    }

    @Override
    public UxItem fromNms(Object nmsItem) {
        if (!(nmsItem instanceof ItemStack stack)) {
            return UxItem.EMPTY;
        }
        return fromMinecraft(stack);
    }

    @Override
    public UxItem empty() {
        return UxItem.EMPTY;
    }

    @Override
    public boolean isEmpty(UxItem item) {
        return item == null || item.isEmpty();
    }

    public ItemStack toMinecraft(UxItem item) {
        if (item == null || item.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (item.hasNativeBukkit()) {
            org.bukkit.inventory.ItemStack nativeStack = item.nativeBukkitClone();
            return nativeStack == null ? ItemStack.EMPTY : CraftItemStack.asNMSCopy(nativeStack);
        }
        ItemStack cached = nmsPrototype(item);
        return cached.isEmpty() ? ItemStack.EMPTY : cached.copy();
    }

    /**
     * Cached NMS prototype — do not mutate. Callers that need a mutable stack must {@code copy()}.
     */
    public ItemStack nmsPrototype(UxItem item) {
        if (item == null || item.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (item.hasNativeBukkit()) {
            return toMinecraft(item);
        }
        ItemStack cached = nmsCache.get(item);
        return cached == null ? ItemStack.EMPTY : cached;
    }

    public UxItem fromMinecraft(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return UxItem.EMPTY;
        }
        return fromBukkit(CraftItemStack.asBukkitCopy(stack));
    }

    @Override
    public org.bukkit.inventory.ItemStack toBukkit(UxItem item) {
        if (item == null || item.isEmpty()) {
            return new org.bukkit.inventory.ItemStack(Material.AIR);
        }
        if (item.hasNativeBukkit()) {
            org.bukkit.inventory.ItemStack nativeStack = item.nativeBukkitClone();
            return nativeStack == null
                    ? new org.bukkit.inventory.ItemStack(Material.AIR)
                    : nativeStack;
        }
        org.bukkit.inventory.ItemStack cached = bukkitCache.get(item);
        return cached == null
                ? new org.bukkit.inventory.ItemStack(Material.AIR)
                : cached.clone();
    }

    @Override
    public void preload(Iterable<UxItem> items) {
        if (items == null) {
            return;
        }
        for (UxItem item : items) {
            if (item == null || item.isEmpty() || item.hasNativeBukkit()) {
                continue;
            }
            BukkitKeyMaps.material(item.materialKey());
            for (String enchantKey : item.enchantments().keySet()) {
                BukkitKeyMaps.enchant(enchantKey);
            }
            bukkitCache.get(item);
            nmsCache.get(item);
        }
    }

    @Override
    public void clearCaches() {
        nmsCache.clear();
        bukkitCache.clear();
    }

    private ItemStack buildMinecraft(UxItem item) {
        org.bukkit.inventory.ItemStack bukkit = buildBukkit(item);
        return CraftItemStack.asNMSCopy(bukkit);
    }

    private org.bukkit.inventory.ItemStack buildBukkit(UxItem item) {
        Material material = BukkitKeyMaps.material(item.materialKey());
        org.bukkit.inventory.ItemStack stack = new org.bukkit.inventory.ItemStack(
                material, Math.max(1, item.amount()));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        if (item.name() != null) {
            meta.displayName(item.name());
        }
        if (!item.lore().isEmpty()) {
            meta.lore(item.lore());
        }
        for (Map.Entry<String, Integer> entry : item.enchantments().entrySet()) {
            Enchantment enchantment = BukkitKeyMaps.enchant(entry.getKey());
            if (enchantment != null) {
                meta.addEnchant(enchantment, entry.getValue(), true);
            }
        }
        if (item.hideEnchantments()) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (item.customModelData() != null) {
            meta.setCustomModelData(item.customModelData());
        }
        if (item.headTextureBase64() != null && !item.headTextureBase64().isEmpty()
                && meta instanceof SkullMeta skull) {
            SkullTextures.applyBase64(skull, item.headTextureBase64());
        }
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public UxItem fromBukkit(org.bukkit.inventory.ItemStack stack) {
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return UxItem.EMPTY;
        }
        NamespacedKey key = stack.getType().getKey();
        ItemMeta meta = stack.getItemMeta();
        Component name = meta != null ? meta.displayName() : null;
        List<Component> lore = meta != null && meta.lore() != null ? meta.lore() : List.of();
        java.util.HashMap<String, Integer> enchants = new java.util.HashMap<>();
        if (meta != null) {
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                enchants.put(entry.getKey().getKey().toString(), entry.getValue());
            }
        }
        boolean hideEnchants = meta != null && meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS);
        Integer cmd = meta != null && meta.hasCustomModelData() ? meta.getCustomModelData() : null;
        String head = null;
        if (meta instanceof SkullMeta skull) {
            head = SkullTextures.extractBase64(skull);
        }
        // Keep a full clone so NBT/DataComponents survive packet send + writeBack.
        return new UxItem(
                key.toString(),
                stack.getAmount(),
                name,
                lore,
                enchants,
                hideEnchants,
                cmd,
                head,
                stack.clone()
        );
    }
}
