package net.opmasterleo.packetuxui.nms.shared;

import java.util.List;
import java.util.Map;

import org.bukkit.craftbukkit.NMS.entity.CraftPlayer;
import org.bukkit.entity.Player;

import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.MenuPacketBridge;
import net.opmasterleo.packetuxui.nms.WindowClickType;
import net.opmasterleo.packetuxui.nms.item.UxItem;

public final class SharedMenuPacketBridge implements MenuPacketBridge {

    private final SharedItemBridge items;

    public SharedMenuPacketBridge(SharedItemBridge items) {
        this.items = items;
    }

    @Override
    public void sendOpenWindow(Player player, int windowId, int typeId, Component title) {
        ServerPlayer sp = nms(player);
        if (sp == null) {
            return;
        }
        sp.connection.send(new ClientboundOpenScreenPacket(
                windowId,
                menuType(typeId),
                net.minecraft.network.chat.Component.Serializer.fromJson(
                        GsonComponentSerializer.gson().serialize(title)
                )
        ));
    }

    @Override
    public void sendCloseWindow(Player player, int windowId) {
        ServerPlayer sp = nms(player);
        if (sp == null) {
            return;
        }
        sp.connection.send(new ClientboundContainerClosePacket(windowId));
    }

    @Override
    public void sendWindowItems(Player player, int windowId, int stateId, List<UxItem> uxItems, UxItem carried) {
        ServerPlayer sp = nms(player);
        if (sp == null) {
            return;
        }
        NonNullList<ItemStack> list = NonNullList.withSize(uxItems.size(), ItemStack.EMPTY);
        for (int i = 0; i < uxItems.size(); i++) {
            list.set(i, items.toMinecraft(uxItems.get(i)));
        }
        ItemStack carriedNms = carried == null ? ItemStack.EMPTY : items.toMinecraft(carried);
        sp.connection.send(new ClientboundContainerSetContentPacket(windowId, stateId, list, carriedNms));
    }

    @Override
    public void sendSetSlot(Player player, int windowId, int stateId, int slot, UxItem item) {
        ServerPlayer sp = nms(player);
        if (sp == null) {
            return;
        }
        sp.connection.send(new ClientboundContainerSetSlotPacket(
                windowId,
                stateId,
                slot,
                items.toMinecraft(item)
        ));
    }

    @Override
    public void injectClick(Player player, ClickPacket click) {
        ServerPlayer sp = nms(player);
        if (sp == null) {
            return;
        }
        Int2ObjectOpenHashMap<ItemStack> changed = new Int2ObjectOpenHashMap<>();
        for (Map.Entry<Integer, UxItem> entry : click.changedSlots().entrySet()) {
            changed.put(entry.getKey().intValue(), items.toMinecraft(entry.getValue()));
        }
        ServerboundContainerClickPacket packet = new ServerboundContainerClickPacket(
                click.windowId(),
                click.slot(),
                click.button(),
                click.stateId(),
                toNmsClickType(click.clickType()),
                items.toMinecraft(click.carried()),
                changed
        );
        sp.connection.handleContainerClick(packet);
    }

    private static ClickType toNmsClickType(WindowClickType type) {
        return switch (type) {
            case PICKUP -> ClickType.PICKUP;
            case QUICK_MOVE -> ClickType.QUICK_MOVE;
            case SWAP -> ClickType.SWAP;
            case CLONE -> ClickType.CLONE;
            case THROW -> ClickType.THROW;
            case QUICK_CRAFT -> ClickType.QUICK_CRAFT;
            case PICKUP_ALL -> ClickType.PICKUP_ALL;
            default -> ClickType.PICKUP;
        };
    }

    private static final MenuType<?>[] MENU_TYPES = createMenuTypes();
    private static final boolean HAS_CRAFTER = MENU_TYPES.length > 24;

    private static MenuType<?>[] createMenuTypes() {
        java.util.ArrayList<MenuType<?>> types = new java.util.ArrayList<>(25);
        types.add(MenuType.GENERIC_9x1);
        types.add(MenuType.GENERIC_9x2);
        types.add(MenuType.GENERIC_9x3);
        types.add(MenuType.GENERIC_9x4);
        types.add(MenuType.GENERIC_9x5);
        types.add(MenuType.GENERIC_9x6);
        types.add(MenuType.GENERIC_3x3);
        MenuType<?> crafter = resolveCrafter();
        if (crafter != null) {
            types.add(crafter);
        }
        types.add(MenuType.ANVIL);
        types.add(MenuType.BEACON);
        types.add(MenuType.BLAST_FURNACE);
        types.add(MenuType.BREWING_STAND);
        types.add(MenuType.CRAFTING);
        types.add(MenuType.ENCHANTMENT);
        types.add(MenuType.FURNACE);
        types.add(MenuType.GRINDSTONE);
        types.add(MenuType.HOPPER);
        types.add(MenuType.LECTERN);
        types.add(MenuType.LOOM);
        types.add(MenuType.MERCHANT);
        types.add(MenuType.SHULKER_BOX);
        types.add(MenuType.SMITHING);
        types.add(MenuType.SMOKER);
        types.add(MenuType.CARTOGRAPHY_TABLE);
        types.add(MenuType.STONECUTTER);
        return types.toArray(MenuType[]::new);
    }

    @SuppressWarnings("unchecked")
    private static MenuType<?> resolveCrafter() {
        try {
            return (MenuType<?>) MenuType.class.getField("CRAFTER_3x3").get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static MenuType<?> menuType(int typeId) {
        // InventoryType ids include CRAFTER at 7 (1.20.3+). Older NMS skips that entry.
        if (!HAS_CRAFTER) {
            if (typeId == 7) {
                return MenuType.GENERIC_3x3;
            }
            if (typeId > 7) {
                typeId--;
            }
        }
        if (typeId >= 0 && typeId < MENU_TYPES.length) {
            return MENU_TYPES[typeId];
        }
        return MenuType.GENERIC_9x6;
    }

    private static ServerPlayer nms(Player player) {
        if (player instanceof CraftPlayer craft) {
            return craft.getHandle();
        }
        return null;
    }
}
