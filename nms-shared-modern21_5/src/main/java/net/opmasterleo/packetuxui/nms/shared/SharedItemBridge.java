package net.opmasterleo.packetuxui.nms.shared;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import net.kyori.adventure.text.Component;
import net.minecraft.world.item.ItemStack;
import net.opmasterleo.packetuxui.nms.ItemBridge;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.nms.map.BukkitKeyMaps;
import net.opmasterleo.packetuxui.nms.map.ConversionCache;

/**
 * Item conversion with runtime mapping caches: material/enchant keys and UxItem→stack
 * prototypes are resolved once, then copied on the hot path.
 */
public final class SharedItemBridge implements ItemBridge {

    private final ConversionCache<UxItem, ItemStack> nmsCache =
            new ConversionCache<>(1024, this::buildMinecraft);
    private final ConversionCache<UxItem, org.bukkit.inventory.ItemStack> bukkitCache =
            new ConversionCache<>(1024, this::buildBukkit);

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
            if (item == null || item.isEmpty()) {
                continue;
            }
            BukkitKeyMaps.material(item.materialKey());
            for (String enchantKey : item.enchantments().keySet()) {
                BukkitKeyMaps.enchant(enchantKey);
            }
            // Bukkit first so NMS build reuses it.
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
        org.bukkit.inventory.ItemStack bukkit = bukkitCache.get(item);
        if (bukkit == null) {
            bukkit = buildBukkit(item);
        }
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
            try {
                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "packetuxui");
                PlayerTextures textures = profile.getTextures();
                skull.setOwnerProfile(profile);
            } catch (Throwable ignored) {
            }
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
        Integer cmd = meta != null && meta.hasCustomModelData() ? meta.getCustomModelData() : null;
        return new UxItem(key.toString(), stack.getAmount(), name, lore, enchants, true, cmd, null);
    }
}
