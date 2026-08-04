package net.opmasterleo.packetuxui.nms.shared;

import java.util.List;
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
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.MenuPacketBridge;
import net.opmasterleo.packetuxui.nms.WindowClickType;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.nms.map.OrdinalMaps;

public final class SharedMenuPacketBridge implements MenuPacketBridge {

    private static final HashedPatchMap.HashGenerator HASH =
            (TypedDataComponent<?> component) -> component.hashCode();

    private static final ClickType[] TO_CLICK;

    static {
        TO_CLICK = new ClickType[WindowClickType.values().length];
        OrdinalMaps.fill(WindowClickType.values(), TO_CLICK, type -> switch (type) {
            case PICKUP -> ClickType.PICKUP;
            case QUICK_MOVE -> ClickType.QUICK_MOVE;
            case SWAP -> ClickType.SWAP;
            case CLONE -> ClickType.CLONE;
            case THROW -> ClickType.THROW;
            case QUICK_CRAFT -> ClickType.QUICK_CRAFT;
            case PICKUP_ALL -> ClickType.PICKUP_ALL;
            default -> ClickType.PICKUP;
        });
    }

    private final SharedItemBridge items;

    public SharedMenuPacketBridge(SharedItemBridge items) {
        this.items = items;
    }

    @Override
    public int allocateWindowId(Player player) {
        ServerPlayer sp = nms(player);
        if (sp == null) {
            return 1;
        }
        return sp.nextContainerCounter();
    }

    @Override
    public int bumpStateId(Player player, int clientFloor) {
        ServerPlayer sp = nms(player);
        if (sp == null) {
            return -1;
        }
        AbstractContainerMenu menu = sp.containerMenu;
        if (!(menu instanceof PacketUxBoundMenu)) {
            return -1;
        }
        int floor = Math.max(0, clientFloor);
        int id = menu.incrementStateId();
        int guard = 0;
        while (id <= floor && guard++ < 10_000) {
            id = menu.incrementStateId();
        }
        return id;
    }

    @Override
    public boolean ownsBoundContainer(Player player) {
        ServerPlayer sp = nms(player);
        return sp != null && sp.containerMenu instanceof PacketUxBoundMenu;
    }

    @Override
    public void mirrorTopSlots(Player player, List<UxItem> topItems) {
        PacketUxBoundMenu bound = bound(player);
        if (bound == null || topItems == null) {
            return;
        }
        SimpleContainer top = bound.topContainer();
        int n = Math.min(top.getContainerSize(), topItems.size());
        for (int i = 0; i < n; i++) {
            UxItem ux = topItems.get(i);
            top.setItem(i, ux == null || ux.isEmpty() ? ItemStack.EMPTY : items.toMinecraft(ux));
        }
    }

    @Override
    public void mirrorSlot(Player player, int slot, UxItem item) {
        PacketUxBoundMenu bound = bound(player);
        if (bound == null || slot < 0) {
            return;
        }
        SimpleContainer top = bound.topContainer();
        if (slot >= top.getContainerSize()) {
            return;
        }
        top.setItem(slot, item == null || item.isEmpty() ? ItemStack.EMPTY : items.toMinecraft(item));
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
    public boolean sendBoundAuthority(Player player, int stateId, boolean clearCursor) {
        ServerPlayer sp = nms(player);
        PacketUxBoundMenu menu = bound(player);
        if (sp == null || menu == null) {
            return false;
        }
        int size = menu.slots.size();
        NonNullList<ItemStack> list = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < size; i++) {
            list.set(i, menu.slots.get(i).getItem());
        }
        if (clearCursor) {
            menu.setCarried(ItemStack.EMPTY);
        }
        ItemStack carried = clearCursor ? ItemStack.EMPTY : menu.getCarried();
        sp.connection.send(new ClientboundContainerSetContentPacket(
                menu.containerId, stateId, list, carried));
        if (clearCursor) {
            sp.connection.send(new ClientboundSetCursorItemPacket(ItemStack.EMPTY));
        }
        return true;
    }

    @Override
    public void sendWindowItems(Player player, int windowId, int stateId, List<UxItem> uxItems, UxItem carried) {
        ServerPlayer sp = nms(player);
        if (sp == null) {
            return;
        }
        PacketUxBoundMenu bound = bound(player);
        int topSize = bound == null ? 0 : bound.topContainer().getContainerSize();
        NonNullList<ItemStack> list = NonNullList.withSize(uxItems.size(), ItemStack.EMPTY);
        for (int i = 0; i < uxItems.size(); i++) {
            ItemStack nmsStack = items.toMinecraft(uxItems.get(i));
            list.set(i, nmsStack);
            if (bound != null && i < topSize) {
                bound.topContainer().setItem(i, nmsStack.copy());
            }
        }
        ItemStack carriedNms = carried == null ? ItemStack.EMPTY : items.toMinecraft(carried);
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
        ItemStack nmsStack = items.toMinecraft(item);
        PacketUxBoundMenu bound = bound(player);
        if (bound != null && slot < bound.topContainer().getContainerSize()) {
            bound.topContainer().setItem(slot, nmsStack.copy());
        }
        sp.connection.send(new ClientboundContainerSetSlotPacket(
                windowId,
                stateId,
                slot,
                nmsStack
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
        if (typeId < 0 || typeId > 5) {
            return;
        }
        ServerPlayer sp = nms(player);
        if (sp == null) {
            return;
        }
        int safeRows = Math.max(1, Math.min(6, rows));
        int top = safeRows * 9;
        SimpleContainer container = new SimpleContainer(top);
        Inventory inv = sp.getInventory();
        MenuType<?> type = menuType(typeId);
        PacketUxBoundMenu menu = new PacketUxBoundMenu(type, windowId, inv, container, safeRows);
        sp.containerMenu = menu;
    }

    @Override
    public void unbindServerContainer(Player player) {
        ServerPlayer sp = nms(player);
        if (sp == null) {
            return;
        }
        AbstractContainerMenu open = sp.containerMenu;
        if (open instanceof PacketUxBoundMenu) {
            sp.containerMenu = sp.inventoryMenu;
        }
    }

    private PacketUxBoundMenu bound(Player player) {
        ServerPlayer sp = nms(player);
        if (sp == null) {
            return null;
        }
        AbstractContainerMenu open = sp.containerMenu;
        return open instanceof PacketUxBoundMenu packetUxBoundMenu ? packetUxBoundMenu : null;
    }

    private static ClickType toNmsClickType(WindowClickType type) {
        if (type == null) {
            return ClickType.PICKUP;
        }
        int i = type.ordinal();
        if (i >= 0 && i < TO_CLICK.length && TO_CLICK[i] != null) {
            return TO_CLICK[i];
        }
        return ClickType.PICKUP;
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

    /**
     * Server-owned ChestMenu: slots cannot pickup/place via vanilla, so even if a
     * click leaks to {@code packet_handler}, the server inventory does not move.
     */
    private static final class PacketUxBoundMenu extends ChestMenu {
        private final SimpleContainer topContainer;

        PacketUxBoundMenu(MenuType<?> type, int windowId, Inventory inv, SimpleContainer container, int rows) {
            super(type, windowId, inv, container, rows);
            this.topContainer = container;
            lockAllSlots();
        }

        SimpleContainer topContainer() {
            return topContainer;
        }

        private void lockAllSlots() {
            for (int i = 0; i < this.slots.size(); i++) {
                Slot old = this.slots.get(i);
                this.slots.set(i, new LockedSlot(old.container, old.getContainerSlot(), old.x, old.y));
            }
        }

        @Override
        public boolean stillValid(net.minecraft.world.entity.player.Player viewer) {
            return true;
        }

        @Override
        public void clicked(int slotId, int buttonId, ClickType clickType, net.minecraft.world.entity.player.Player viewer) {
            setCarried(ItemStack.EMPTY);
        }

        @Override
        public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player viewer, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
            return false;
        }
    }

    private static final class LockedSlot extends Slot {
        LockedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
            return false;
        }

        @Override
        public ItemStack remove(int amount) {
            return ItemStack.EMPTY;
        }
    }
}
