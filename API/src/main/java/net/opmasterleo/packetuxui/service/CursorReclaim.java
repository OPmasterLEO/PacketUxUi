package net.opmasterleo.packetuxui.service;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.opmasterleo.packetuxui.nms.ItemBridge;
import net.opmasterleo.packetuxui.nms.item.UxItem;

public final class CursorReclaim {

    private CursorReclaim() {
    }

    public static void reclaim(Player player, ItemBridge items, UxItem cursor) {
        if (player == null || items == null || cursor == null || cursor.isEmpty()) {
            return;
        }
        ItemStack stack = items.toBukkit(cursor);
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return;
        }
        PlayerInventory inv = player.getInventory();
        HashMap<Integer, ItemStack> leftover = inv.addItem(stack);
        if (leftover.isEmpty() || player.getWorld() == null) {
            return;
        }
        for (ItemStack drop : leftover.values()) {
            if (drop != null && !drop.getType().isAir() && drop.getAmount() > 0) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    public static void reclaimMap(Player player, ItemBridge items, Map<Integer, UxItem> slots) {
        if (slots == null || slots.isEmpty()) {
            return;
        }
        for (UxItem item : slots.values()) {
            reclaim(player, items, item);
        }
    }
}
