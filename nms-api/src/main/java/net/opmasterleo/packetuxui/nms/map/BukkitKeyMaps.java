package net.opmasterleo.packetuxui.nms.map;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

/**
 * Runtime key→Bukkit resolutions, computed once then O(1) on the hot path.
 * Same idea as versioned mapping tables: resolve at first use (or {@link #warmup}),
 * never re-parse material/enchant keys on every slot convert.
 */
public final class BukkitKeyMaps {

    private static final ConcurrentMap<String, Material> MATERIALS = new ConcurrentHashMap<>(256);
    private static final ConcurrentMap<String, Enchantment> ENCHANTS = new ConcurrentHashMap<>(64);
    private static final Material AIR = Material.AIR;

    private BukkitKeyMaps() {
    }

    public static Material material(String key) {
        if (key == null || key.isEmpty()) {
            return AIR;
        }
        Material cached = MATERIALS.get(key);
        if (cached != null) {
            return cached;
        }
        return MATERIALS.computeIfAbsent(key, BukkitKeyMaps::resolveMaterial);
    }

    public static Enchantment enchant(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        return ENCHANTS.computeIfAbsent(key, BukkitKeyMaps::resolveEnchant);
    }

    /** Eagerly map keys that menus use often (pane fillers, buttons, etc.). */
    public static void warmup(Iterable<String> materialKeys) {
        if (materialKeys == null) {
            return;
        }
        for (String key : materialKeys) {
            material(key);
        }
    }

    public static void warmupDefaults() {
        warmup(java.util.List.of(
                "minecraft:air",
                "minecraft:gray_stained_glass_pane",
                "minecraft:black_stained_glass_pane",
                "minecraft:glass_pane",
                "minecraft:arrow",
                "minecraft:barrier",
                "minecraft:book",
                "minecraft:paper",
                "minecraft:chest",
                "minecraft:ender_chest",
                "minecraft:player_head",
                "minecraft:gold_ingot",
                "minecraft:emerald",
                "minecraft:nether_star"
        ));
    }

    private static Material resolveMaterial(String key) {
        String normalized = stripNamespace(key);
        Material material = Material.matchMaterial(normalized);
        if (material == null) {
            material = Material.matchMaterial(normalized, false);
        }
        return material == null ? AIR : material;
    }

    private static Enchantment resolveEnchant(String key) {
        String normalized = stripNamespace(key).toLowerCase(Locale.ROOT);
        NamespacedKey namespacedKey = NamespacedKey.minecraft(normalized);
        return Enchantment.getByKey(namespacedKey);
    }

    private static String stripNamespace(String key) {
        int idx = key.indexOf(':');
        return idx >= 0 ? key.substring(idx + 1) : key;
    }
}
