package net.opmasterleo.packetuxui.nms.shared;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import io.papermc.paper.adventure.PaperAdventure;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kyori.adventure.text.Component;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.MenuPacketBridge;
import net.opmasterleo.packetuxui.nms.WindowClickType;
import net.opmasterleo.packetuxui.nms.item.UxItem;

public final class SharedMenuPacketBridge implements MenuPacketBridge {

    private static final HashedPatchMap.HashGenerator HASH =
            (TypedDataComponent<?> component) -> component.hashCode();

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
                PaperAdventure.asVanilla(title)
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
        Map<UxItem, ItemStack> converted = new HashMap<>();
        for (int i = 0; i < uxItems.size(); i++) {
            UxItem ux = uxItems.get(i);
            ItemStack nms = converted.get(ux);
            if (nms == null) {
                nms = items.toMinecraft(ux);
                converted.put(ux, nms);
            }
            list.set(i, nms.copy());
        }
        ItemStack carriedNms;
        if (carried == null || carried.isEmpty()) {
            carriedNms = ItemStack.EMPTY;
        } else {
            ItemStack cached = converted.get(carried);
            carriedNms = cached == null ? items.toMinecraft(carried) : cached.copy();
        }
        sp.connection.send(new ClientboundContainerSetContentPacket(windowId, stateId, list, carriedNms));
    }

    @Override
    public void sendSetSlot(Player player, int windowId, int stateId, int slot, UxItem item) {
        if (slot < 0) {
            sendCursorItem(player, item);
            return;
        }
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
    public void sendCursorItem(Player player, UxItem item) {
        ServerPlayer sp = nms(player);
        if (sp == null) {
            return;
        }
        sp.connection.send(new ClientboundSetCursorItemPacket(items.toMinecraft(item)));
    }

    @Override
    public void injectClick(Player player, ClickPacket click) {
        ServerPlayer sp = nms(player);
        if (sp == null) {
            return;
        }
        Int2ObjectOpenHashMap<HashedStack> changed = new Int2ObjectOpenHashMap<>();
        for (Map.Entry<Integer, UxItem> entry : click.changedSlots().entrySet()) {
            changed.put(entry.getKey().intValue(), HashedStack.create(items.toMinecraft(entry.getValue()), HASH));
        }
        ServerboundContainerClickPacket packet = new ServerboundContainerClickPacket(
                click.windowId(),
                click.stateId(),
                (short) click.slot(),
                (byte) click.button(),
                toNmsClickType(click.clickType()),
                changed,
                HashedStack.create(items.toMinecraft(click.carried()), HASH)
        );
        sp.connection.handleContainerClick(packet);
    }

    @Override
    public void bindServerContainer(Player player, int windowId, int typeId, int rows) {
        ServerPlayer sp = nms(player);
        if (sp == null) {
            return;
        }
        int safeRows = Math.max(1, Math.min(6, rows));
        int top = safeRows * 9;
        SimpleContainer container = new SimpleContainer(top);
        Inventory inv = sp.getInventory();
        MenuType<?> type = menuType(typeId);
        ChestMenu menu = new ChestMenu(type, windowId, inv, container, safeRows) {
            @Override
            public boolean stillValid(net.minecraft.world.entity.player.Player viewer) {
                return true;
            }

            @Override
            public void clicked(int slotId, int buttonId, ContainerInput clickType, net.minecraft.world.entity.player.Player viewer) {
                // PacketUxUi owns click handling; keep the bound container inert for AC validity only.
            }

            @Override
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player viewer, int index) {
                return ItemStack.EMPTY;
            }
        };
        sp.containerMenu = menu;
    }

    @Override
    public void unbindServerContainer(Player player) {
        ServerPlayer sp = nms(player);
        if (sp == null) {
            return;
        }
        AbstractContainerMenu open = sp.containerMenu;
        if (open == null || open == sp.inventoryMenu) {
            return;
        }
        int id = open.containerId;
        // Only reclaim menus we bound for virtual windows (ids 100-126).
        if (id >= 100 && id <= 126) {
            sp.containerMenu = sp.inventoryMenu;
        }
    }

    private static ContainerInput toNmsClickType(WindowClickType type) {
        return switch (type) {
            case PICKUP -> ContainerInput.PICKUP;
            case QUICK_MOVE -> ContainerInput.QUICK_MOVE;
            case SWAP -> ContainerInput.SWAP;
            case CLONE -> ContainerInput.CLONE;
            case THROW -> ContainerInput.THROW;
            case QUICK_CRAFT -> ContainerInput.QUICK_CRAFT;
            case PICKUP_ALL -> ContainerInput.PICKUP_ALL;
            default -> ContainerInput.PICKUP;
        };
    }

    private static final MenuType<?>[] MENU_TYPES = {
            MenuType.GENERIC_9x1,
            MenuType.GENERIC_9x2,
            MenuType.GENERIC_9x3,
            MenuType.GENERIC_9x4,
            MenuType.GENERIC_9x5,
            MenuType.GENERIC_9x6,
            MenuType.GENERIC_3x3,
            MenuType.CRAFTER_3x3,
            MenuType.ANVIL,
            MenuType.BEACON,
            MenuType.BLAST_FURNACE,
            MenuType.BREWING_STAND,
            MenuType.CRAFTING,
            MenuType.ENCHANTMENT,
            MenuType.FURNACE,
            MenuType.GRINDSTONE,
            MenuType.HOPPER,
            MenuType.LECTERN,
            MenuType.LOOM,
            MenuType.MERCHANT,
            MenuType.SHULKER_BOX,
            MenuType.SMITHING,
            MenuType.SMOKER,
            MenuType.CARTOGRAPHY_TABLE,
            MenuType.STONECUTTER
    };

    private static MenuType<?> menuType(int typeId) {
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
