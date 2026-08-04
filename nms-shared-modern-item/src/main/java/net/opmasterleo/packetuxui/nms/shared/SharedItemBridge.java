package net.opmasterleo.packetuxui.nms.shared;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

public final class SharedItemBridge implements ItemBridge {

    private static final int NMS_CACHE_LIMIT = 256;
    private final ConcurrentHashMap<UxItem, ItemStack> nmsCache = new ConcurrentHashMap<>();

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
        ItemStack cached = nmsCache.get(item);
        if (cached != null) {
            return cached.copy();
        }
        org.bukkit.inventory.ItemStack bukkit = toBukkit(item);
        ItemStack nms = CraftItemStack.asNMSCopy(bukkit);
        if (nmsCache.size() >= NMS_CACHE_LIMIT) {
            nmsCache.clear();
        }
        nmsCache.put(item, nms.copy());
        return nms;
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
        Material material = resolveMaterial(item.materialKey());
        org.bukkit.inventory.ItemStack stack = new org.bukkit.inventory.ItemStack(material, Math.max(1, item.amount()));
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
            Enchantment enchantment = resolveEnchant(entry.getKey());
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

    private static Material resolveMaterial(String key) {
        String normalized = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        Material material = Material.matchMaterial(normalized);
        return material == null ? Material.AIR : material;
    }

    private static Enchantment resolveEnchant(String key) {
        String normalized = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        NamespacedKey namespacedKey = NamespacedKey.minecraft(normalized);
        return Enchantment.getByKey(namespacedKey);
    }
}
