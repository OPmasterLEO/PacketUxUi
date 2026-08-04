package net.opmasterleo.packetuxui.nms.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.craftbukkit.NMS.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.world.item.ItemStack;
import net.opmasterleo.packetuxui.nms.ItemBridge;
import net.opmasterleo.packetuxui.nms.item.UxItem;

public final class SharedItemBridge implements ItemBridge {

    private static final int NMS_CACHE_LIMIT = 256;
    private final java.util.concurrent.ConcurrentHashMap<UxItem, ItemStack> nmsCache =
            new java.util.concurrent.ConcurrentHashMap<>();

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
        ItemStack nms = CraftItemStack.asNMSCopy(toBukkit(item));
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
            meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(item.name()));
        }
        if (!item.lore().isEmpty()) {
            ArrayList<String> lore = new ArrayList<>();
            for (Component line : item.lore()) {
                lore.add(LegacyComponentSerializer.legacySection().serialize(line));
            }
            meta.setLore(lore);
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
        stack.setItemMeta(meta);
        return stack;
    }

    public UxItem fromBukkit(org.bukkit.inventory.ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) {
            return UxItem.EMPTY;
        }
        String key = "minecraft:" + stack.getType().name().toLowerCase();
        ItemMeta meta = stack.getItemMeta();
        Component name = null;
        List<Component> lore = List.of();
        Map<String, Integer> enchants = new HashMap<>();
        boolean hideEnchants = false;
        if (meta != null) {
            if (meta.hasDisplayName()) {
                name = LegacyComponentSerializer.legacySection().deserialize(meta.getDisplayName());
            }
            if (meta.hasLore() && meta.getLore() != null) {
                ArrayList<Component> lines = new ArrayList<>();
                for (String line : meta.getLore()) {
                    lines.add(LegacyComponentSerializer.legacySection().deserialize(line));
                }
                lore = lines;
            }
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                enchants.put(entry.getKey().getName().toLowerCase(), entry.getValue());
            }
            hideEnchants = meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS);
        }
        return new UxItem(key, stack.getAmount(), name, lore, enchants, hideEnchants, null, null, stack.clone());
    }

    private static Material resolveMaterial(String key) {
        String normalized = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        Material material = Material.matchMaterial(normalized);
        if (material != null) {
            return material;
        }
        try {
            return Material.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return Material.AIR;
        }
    }

    private static Enchantment resolveEnchant(String key) {
        String normalized = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        return Enchantment.getByName(normalized.toUpperCase());
    }
}
