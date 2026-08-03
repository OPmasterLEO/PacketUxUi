package net.opmasterleo.packetuxui.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.opmasterleo.packetuxui.dto.AccumulatedDrag;
import net.opmasterleo.packetuxui.dto.CooldownComponent;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.NmsAdapter;
import net.opmasterleo.packetuxui.nms.WindowClickType;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.types.ButtonType;
import net.opmasterleo.packetuxui.types.ClickData;
import net.opmasterleo.packetuxui.types.ClickType;
import net.opmasterleo.packetuxui.types.ExecuteComponent;

public final class MenuService {

    /** @deprecated Use {@link WindowIdPool} per-player ids. */
    @Deprecated
    public static final int WINDOW_ID = WindowIdPool.LEGACY_FIXED_ID;

    private final NmsAdapter adapter;
    private final PlatformScheduler scheduler;
    private final WindowIdPool windowIds = new WindowIdPool();
    private final ConcurrentHashMap<UUID, MenuSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UxItem> carriedItem = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<AccumulatedDrag>> accumulatedDrag = new ConcurrentHashMap<>();

    public MenuService(NmsAdapter adapter, PlatformScheduler scheduler) {
        this.adapter = adapter;
        this.scheduler = scheduler;
    }

    private static UUID id(Player player) {
        return player.getUniqueId();
    }

    public void openMenu(Player player, Menu menu) {
        scheduler.runForPlayer(player, () -> {
            closeCurrent(player, true);
            Menu copy = menu.copy();
            int windowId = windowIds.allocate(player);
            MenuSession session = new MenuSession(copy, windowId);
            sessions.put(id(player), session);
            int stateId = session.nextStateId();
            adapter.packets().sendOpenWindow(player, windowId, copy.type().id(), copy.name());
            adapter.packets().sendWindowItems(player, windowId, stateId, contentsForOpen(player, copy), null);
        });
    }

    public void onCloseMenu(Player player) {
        closeCurrent(player, false);
    }

    public void closeMenu(Player player) {
        scheduler.runForPlayer(player, () -> closeCurrent(player, true));
    }

    private void closeCurrent(Player player, boolean sendClosePacket) {
        MenuSession session = sessions.get(id(player));
        if (session == null) {
            windowIds.release(player);
            carriedItem.remove(id(player));
            clearAccumulatedDrag(player);
            return;
        }
        UxItem cursor = carriedItem.getOrDefault(id(player), UxItem.EMPTY);
        BiConsumer<Player, CloseSnapshot> onClose = session.menu().onClose();
        if (onClose != null) {
            try {
                onClose.accept(player, new CloseSnapshot(session.menu().items(), cursor));
            } catch (Throwable ignored) {
            }
        }
        if (sendClosePacket) {
            adapter.packets().sendCloseWindow(player, session.windowId());
        }
        sessions.remove(id(player));
        windowIds.release(player);
        carriedItem.remove(id(player));
        clearAccumulatedDrag(player);
    }

    public void handleIncomingClick(Player player, ClickPacket packet) {
        ClickData clickData = getClickType(packet);
        if (clickData.clickType() == ClickType.DRAG_START || clickData.clickType() == ClickType.DRAG_ADD) {
            accumulateDrag(player, packet, clickData.clickType());
            return;
        }
        MenuSession session = sessions.get(id(player));
        if (session == null) {
            return;
        }
        if (session.menu().mode() == MenuMode.EDITABLE) {
            handleEditableClick(player, session, clickData, packet);
            return;
        }
        if (isMenuClick(packet, clickData.clickType(), player)) {
            handleClickMenu(player, session, clickData, packet);
            return;
        }
        if (session.menu().mode() == MenuMode.EDITABLE_PLAYER_INVENTORY) {
            handleClickInventory(player, session, packet);
            return;
        }
        settleReadOnlyOutside(player, session, packet);
    }

    private void settleReadOnlyOutside(Player player, MenuSession session, ClickPacket packet) {
        UxItem packetCursor = packet.carried();
        boolean emptyCursor = packetCursor == null || adapter.items().isEmpty(packetCursor);
        Menu menu = session.menu();
        int last = menu.type().lastIndex();
        boolean touchesTop = (packet.slot() >= 0 && packet.slot() <= last)
                || packet.changedSlots().keySet().stream().anyMatch(s -> s != null && s >= 0 && s <= last);
        if (!touchesTop && emptyCursor && packet.changedSlots().isEmpty()) {
            int stateId = session.nextStateId();
            adapter.packets().sendSetSlot(player, session.windowId(), stateId, -1, UxItem.EMPTY);
            carriedItem.remove(id(player));
            return;
        }
        if (!touchesTop) {
            settleBottomSlots(player, session, packet);
            return;
        }
        resyncDirtySlots(player, session, packet, UxItem.EMPTY);
        carriedItem.remove(id(player));
    }

    private void settleBottomSlots(Player player, MenuSession session, ClickPacket packet) {
        Menu menu = session.menu();
        int topSize = menu.type().size();
        int last = menu.type().lastIndex();
        List<UxItem> bottom = snapshotBottom(player);
        int windowId = session.windowId();
        int stateId = session.nextStateId();
        Set<Integer> dirty = new HashSet<>();
        if (packet.slot() > last) {
            dirty.add(packet.slot());
        }
        for (Integer slot : packet.changedSlots().keySet()) {
            if (slot != null && slot > last) {
                dirty.add(slot);
            }
        }
        for (Integer slot : dirty) {
            int idx = slot - topSize;
            if (idx >= 0 && idx < bottom.size()) {
                adapter.packets().sendSetSlot(player, windowId, stateId, slot, bottom.get(idx));
            }
        }
        adapter.packets().sendSetSlot(player, windowId, stateId, -1, UxItem.EMPTY);
        carriedItem.remove(id(player));
    }

    private void handleEditableClick(Player player, MenuSession session, ClickData clickData, ClickPacket packet) {
        Menu menu = session.menu();
        int last = menu.type().lastIndex();
        WindowClickType type = packet.clickType();

        if (type == WindowClickType.SWAP || type == WindowClickType.THROW || type == WindowClickType.CLONE) {
            rejectEditable(player, session, packet);
            return;
        }

        if (clickData.clickType() == ClickType.DRAG_END) {
            handleEditableDragEnd(player, session, packet);
            return;
        }

        boolean topSlot = packet.slot() >= 0 && packet.slot() <= last;
        if (!topSlot) {
            resyncFull(player, session);
            return;
        }

        Button button = menu.buttons().get(packet.slot());
        if (button != null && !button.takeable()) {
            fireLockedButton(player, session, clickData, packet, button);
            return;
        }

        Predicate<Integer> takeable = slot -> {
            if (slot < 0 || slot > last) {
                return false;
            }
            Button b = menu.buttons().get(slot);
            return b == null || b.takeable();
        };

        UxItem cursor = carriedItem.getOrDefault(id(player), UxItem.EMPTY);
        VirtualClickSimulator.Result result = VirtualClickSimulator.simulate(
                menu.items(),
                cursor,
                packet.slot(),
                packet.button(),
                type,
                takeable
        );
        applyEditableResult(player, session, result);
    }

    private void handleEditableDragEnd(Player player, MenuSession session, ClickPacket packet) {
        Menu menu = session.menu();
        int last = menu.type().lastIndex();
        Predicate<Integer> takeable = slot -> {
            if (slot < 0 || slot > last) {
                return false;
            }
            Button b = menu.buttons().get(slot);
            return b == null || b.takeable();
        };
        List<Integer> slots = new ArrayList<>();
        List<AccumulatedDrag> drags = accumulatedDrag.get(id(player));
        if (drags != null) {
            for (AccumulatedDrag drag : drags) {
                if (drag.type() == ClickType.DRAG_ADD) {
                    slots.add(drag.packet().slot());
                }
            }
        }
        for (Integer slot : packet.changedSlots().keySet()) {
            if (slot != null) {
                slots.add(slot);
            }
        }
        clearAccumulatedDrag(player);
        if (packet.button() == 10) {
            rejectEditable(player, session, packet);
            return;
        }
        UxItem cursor = carriedItem.getOrDefault(id(player), UxItem.EMPTY);
        VirtualClickSimulator.Result result = VirtualClickSimulator.dragEnd(
                menu.items(),
                cursor,
                slots,
                packet.button(),
                takeable
        );
        applyEditableResult(player, session, result);
    }

    private void fireLockedButton(
            Player player,
            MenuSession session,
            ClickData clickData,
            ClickPacket packet,
            Button button
    ) {
        if (clickData.clickType() == ClickType.DRAG_END) {
            clearAccumulatedDrag(player);
        }
        UxItem cursor = carriedItem.getOrDefault(id(player), UxItem.EMPTY);
        resyncDirtySlots(player, session, packet, cursor);
        if (button.execute() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Menu menu = session.menu();
        CooldownComponent cooldown = button.cooldown().combine(menu.cooldown());
        Consumer<ExecuteComponent> execute;
        if (!cooldown.isFreezeExpired(now)) {
            execute = null;
        } else if (!cooldown.isTimeExpired(now)) {
            button.cooldown().resetFreeze();
            execute = button.cooldown().execute();
        } else {
            button.cooldown().resetTime();
            execute = button.execute();
        }
        if (execute != null) {
            execute.accept(new ExecuteComponent(player, clickData.buttonType(), packet.slot(), cursor));
        }
    }

    private void applyEditableResult(Player player, MenuSession session, VirtualClickSimulator.Result result) {
        Menu menu = session.menu();
        menu.setItems(result.items());
        if (result.cursor().isEmpty()) {
            carriedItem.remove(id(player));
        } else {
            carriedItem.put(id(player), result.cursor());
        }
        int windowId = session.windowId();
        int stateId = session.nextStateId();
        for (Integer slot : result.dirty()) {
            UxItem item = slot < result.items().size() ? result.items().get(slot) : UxItem.EMPTY;
            adapter.packets().sendSetSlot(player, windowId, stateId, slot, item);
        }
        adapter.packets().sendSetSlot(player, windowId, stateId, -1, result.cursor());
    }

    private void rejectEditable(Player player, MenuSession session, ClickPacket packet) {
        Menu menu = session.menu();
        int last = menu.type().lastIndex();
        int windowId = session.windowId();
        int stateId = session.nextStateId();
        UxItem cursor = carriedItem.getOrDefault(id(player), UxItem.EMPTY);
        if (packet.slot() >= 0 && packet.slot() <= last) {
            UxItem item = packet.slot() < menu.items().size() ? menu.items().get(packet.slot()) : UxItem.EMPTY;
            adapter.packets().sendSetSlot(player, windowId, stateId, packet.slot(), item);
        }
        adapter.packets().sendSetSlot(player, windowId, stateId, -1, cursor);
    }

    private void resyncFull(Player player, MenuSession session) {
        int stateId = session.nextStateId();
        UxItem cursor = carriedItem.getOrDefault(id(player), UxItem.EMPTY);
        adapter.packets().sendWindowItems(
                player,
                session.windowId(),
                stateId,
                fullContents(player, session.menu()),
                cursor.isEmpty() ? null : cursor
        );
    }

    private List<UxItem> contentsForOpen(Player player, Menu menu) {
        if (menu.mode() == MenuMode.EDITABLE) {
            return fullContents(player, menu);
        }
        return menu.items();
    }

    private List<UxItem> fullContents(Player player, Menu menu) {
        List<UxItem> contents = new ArrayList<>(menu.type().size() + 36);
        contents.addAll(menu.items());
        contents.addAll(snapshotBottom(player));
        return contents;
    }

    private List<UxItem> snapshotBottom(Player player) {
        List<UxItem> bottom = new ArrayList<>(36);
        PlayerInventory inv = player.getInventory();
        for (int i = 9; i < 36; i++) {
            bottom.add(fromBukkitSlot(inv.getItem(i)));
        }
        for (int i = 0; i < 9; i++) {
            bottom.add(fromBukkitSlot(inv.getItem(i)));
        }
        return bottom;
    }

    private UxItem fromBukkitSlot(ItemStack stack) {
        if (stack == null) {
            return UxItem.EMPTY;
        }
        return adapter.items().fromBukkit(stack);
    }

    public void handleClickInventory(Player player, ClickPacket packet) {
        MenuSession session = requireSession(player);
        handleClickInventory(player, session, packet);
    }

    private void handleClickInventory(Player player, MenuSession session, ClickPacket packet) {
        Menu menu = session.menu();
        ClickData clickData = getClickType(packet);
        updateCarriedItem(player, packet.carried(), clickData.clickType());
        if (clickData.clickType() == ClickType.DRAG_END) {
            handleDragEnd(player, menu);
        }
        adapter.packets().injectClick(player, createAdjustedClickPacket(packet, menu));
    }

    public void handleClickMenu(Player player, ClickData clickData, int slot) {
        MenuSession session = requireSession(player);
        handleClickMenu(player, session, clickData, syntheticClick(session, slot));
    }

    private void handleClickMenu(Player player, MenuSession session, ClickData clickData, ClickPacket packet) {
        if (clickData.clickType() == ClickType.DRAG_END) {
            clearAccumulatedDrag(player);
        }
        Menu menu = session.menu();
        int slot = packet.slot();
        Button button = slot >= 0 ? menu.buttons().get(slot) : null;
        resyncDirtySlots(player, session, packet, UxItem.EMPTY);
        carriedItem.remove(id(player));
        if (button == null) {
            return;
        }
        long now = System.currentTimeMillis();
        CooldownComponent cooldown = button.cooldown().combine(menu.cooldown());
        Consumer<ExecuteComponent> execute;
        if (!cooldown.isFreezeExpired(now)) {
            execute = null;
        } else if (!cooldown.isTimeExpired(now)) {
            button.cooldown().resetFreeze();
            execute = button.cooldown().execute();
        } else {
            button.cooldown().resetTime();
            execute = button.execute();
        }
        if (execute != null) {
            execute.accept(new ExecuteComponent(player, clickData.buttonType(), slot, UxItem.EMPTY));
        }
    }

    public void refreshWindow(Player player) {
        scheduler.runForPlayer(player, () -> {
            MenuSession session = sessions.get(id(player));
            if (session == null) {
                return;
            }
            int stateId = session.nextStateId();
            List<UxItem> contents = session.menu().mode() == MenuMode.EDITABLE
                    ? fullContents(player, session.menu())
                    : session.menu().items();
            adapter.packets().sendWindowItems(
                    player,
                    session.windowId(),
                    stateId,
                    contents,
                    carriedItem.get(id(player))
            );
        });
    }

    public void updateItem(Player player, UxItem item, int slot) {
        scheduler.runForPlayer(player, () -> {
            MenuSession session = sessions.get(id(player));
            if (session == null) {
                return;
            }
            Menu menu = session.menu();
            if (slot > menu.type().lastIndex()) {
                throw new IllegalArgumentException("Slot out of range.");
            }
            UxItem next = item == null ? UxItem.EMPTY : item;
            List<UxItem> items = menu.items();
            UxItem current = slot < items.size() ? items.get(slot) : UxItem.EMPTY;
            if (current.equals(next)) {
                return;
            }
            List<UxItem> copy = new ArrayList<>(items);
            copy.set(slot, next);
            menu.setItems(copy);
            adapter.packets().sendSetSlot(player, session.windowId(), session.nextStateId(), slot, next);
        });
    }

    public void updateItems(Player player, Map<Integer, UxItem> newItems) {
        scheduler.runForPlayer(player, () -> {
            MenuSession session = sessions.get(id(player));
            if (session == null) {
                return;
            }
            Menu menu = session.menu();
            for (Integer slot : newItems.keySet()) {
                if (slot > menu.type().lastIndex()) {
                    throw new IllegalArgumentException("Slot out of range.");
                }
            }
            List<UxItem> items = new ArrayList<>(menu.items());
            Map<Integer, UxItem> dirty = new HashMap<>();
            for (Map.Entry<Integer, UxItem> entry : newItems.entrySet()) {
                UxItem next = entry.getValue() == null ? UxItem.EMPTY : entry.getValue();
                UxItem current = entry.getKey() < items.size() ? items.get(entry.getKey()) : UxItem.EMPTY;
                if (current.equals(next)) {
                    continue;
                }
                items.set(entry.getKey(), next);
                dirty.put(entry.getKey(), next);
            }
            if (dirty.isEmpty()) {
                return;
            }
            menu.setItems(items);
            int stateId = session.nextStateId();
            for (Map.Entry<Integer, UxItem> entry : dirty.entrySet()) {
                adapter.packets().sendSetSlot(
                        player,
                        session.windowId(),
                        stateId,
                        entry.getKey(),
                        entry.getValue()
                );
            }
        });
    }

    public void updateButton(Player player, Button newButton, int slot) {
        scheduler.runForPlayer(player, () -> {
            MenuSession session = sessions.get(id(player));
            if (session == null) {
                return;
            }
            Menu menu = session.menu();
            if (slot > menu.type().lastIndex()) {
                throw new IllegalArgumentException("Slot out of range.");
            }
            menu.buttons().put(slot, newButton);
            UxItem next = newButton.item();
            List<UxItem> items = new ArrayList<>(menu.items());
            UxItem current = slot < items.size() ? items.get(slot) : UxItem.EMPTY;
            items.set(slot, next);
            menu.setItems(items);
            if (current.equals(next)) {
                return;
            }
            adapter.packets().sendSetSlot(player, session.windowId(), session.nextStateId(), slot, next);
        });
    }

    public void updateButtons(Player player, Map<Integer, Button> newButtons) {
        scheduler.runForPlayer(player, () -> {
            MenuSession session = sessions.get(id(player));
            if (session == null) {
                return;
            }
            Menu menu = session.menu();
            for (Integer slot : newButtons.keySet()) {
                if (slot > menu.type().lastIndex()) {
                    throw new IllegalArgumentException("Slot out of range.");
                }
            }
            menu.buttons().clear();
            menu.buttons().putAll(newButtons);
            List<UxItem> items = new ArrayList<>(menu.type().size());
            List<UxItem> previous = menu.items();
            Map<Integer, UxItem> dirty = new HashMap<>();
            for (int index = 0; index < menu.type().size(); index++) {
                Button button = newButtons.get(index);
                UxItem next = button != null ? button.item() : UxItem.EMPTY;
                items.add(next);
                UxItem current = index < previous.size() ? previous.get(index) : UxItem.EMPTY;
                if (!current.equals(next)) {
                    dirty.put(index, next);
                }
            }
            menu.setItems(items);
            if (dirty.isEmpty()) {
                return;
            }
            int stateId = session.nextStateId();
            int windowId = session.windowId();
            for (Map.Entry<Integer, UxItem> entry : dirty.entrySet()) {
                adapter.packets().sendSetSlot(player, windowId, stateId, entry.getKey(), entry.getValue());
            }
        });
    }

    public Menu getMenu(Player player) {
        MenuSession session = sessions.get(id(player));
        return session == null ? null : session.menu();
    }

    public MenuSession getSession(Player player) {
        return sessions.get(id(player));
    }

    public boolean shouldIgnore(int id, Player player) {
        MenuSession session = sessions.get(id(player));
        return session == null || session.windowId() != id;
    }

    public boolean isMenuClick(ClickPacket packet, ClickType clickType, Player player) {
        Menu menu = requireMenu(player);
        int last = menu.type().lastIndex();
        return switch (clickType) {
            case SHIFT_CLICK -> true;
            case PICKUP, PLACE -> packet.slot() >= 0 && packet.slot() <= last;
            case DRAG_END, PICKUP_ALL -> (packet.slot() >= 0 && packet.slot() <= last)
                    || packet.changedSlots().keySet().stream().anyMatch(slot -> slot >= 0 && slot <= last);
            default -> false;
        };
    }

    public ClickData getClickType(ClickPacket packet) {
        return switch (packet.clickType()) {
            case PICKUP -> {
                UxItem carried = packet.carried();
                boolean isCarried = carried != null && !adapter.items().isEmpty(carried);
                if (packet.button() == 0) {
                    yield new ClickData(ButtonType.LEFT, isCarried ? ClickType.PICKUP : ClickType.PLACE);
                }
                yield new ClickData(ButtonType.RIGHT, isCarried ? ClickType.PLACE : ClickType.PICKUP);
            }
            case QUICK_MOVE -> packet.button() == 0
                    ? new ClickData(ButtonType.SHIFT_LEFT, ClickType.SHIFT_CLICK)
                    : new ClickData(ButtonType.SHIFT_RIGHT, ClickType.SHIFT_CLICK);
            case SWAP -> {
                int button = packet.button();
                if (button >= 0 && button <= 8) {
                    yield new ClickData(ButtonType.values()[9 + button], ClickType.PICKUP);
                }
                if (button == 40) {
                    yield new ClickData(ButtonType.F, ClickType.PICKUP);
                }
                yield new ClickData(ButtonType.LEFT, ClickType.PLACE);
            }
            case CLONE -> new ClickData(ButtonType.MIDDLE, ClickType.PICKUP);
            case THROW -> packet.button() == 0
                    ? new ClickData(ButtonType.DROP, ClickType.PICKUP)
                    : new ClickData(ButtonType.CTRL_DROP, ClickType.PICKUP);
            case QUICK_CRAFT -> switch (packet.button()) {
                case 0 -> new ClickData(ButtonType.LEFT, ClickType.DRAG_START);
                case 4 -> new ClickData(ButtonType.RIGHT, ClickType.DRAG_START);
                case 8 -> new ClickData(ButtonType.MIDDLE, ClickType.DRAG_START);
                case 1 -> new ClickData(ButtonType.LEFT, ClickType.DRAG_ADD);
                case 5 -> new ClickData(ButtonType.RIGHT, ClickType.DRAG_ADD);
                case 9 -> new ClickData(ButtonType.MIDDLE, ClickType.DRAG_ADD);
                case 2 -> new ClickData(ButtonType.LEFT, ClickType.DRAG_END);
                case 6 -> new ClickData(ButtonType.RIGHT, ClickType.DRAG_END);
                case 10 -> new ClickData(ButtonType.MIDDLE, ClickType.DRAG_END);
                default -> new ClickData(ButtonType.LEFT, ClickType.UNDEFINED);
            };
            case PICKUP_ALL -> new ClickData(ButtonType.DOUBLE_CLICK, ClickType.PICKUP_ALL);
            default -> new ClickData(ButtonType.LEFT, ClickType.UNDEFINED);
        };
    }

    public void accumulateDrag(Player player, ClickPacket packet, ClickType type) {
        accumulatedDrag.computeIfAbsent(id(player), key -> new ArrayList<>()).add(new AccumulatedDrag(packet, type));
    }

    private void handleDragEnd(Player player, Menu menu) {
        List<AccumulatedDrag> drags = accumulatedDrag.get(id(player));
        if (drags != null) {
            for (AccumulatedDrag drag : drags) {
                ClickPacket packet = drag.type() == ClickType.DRAG_START
                        ? createDragPacket(drag.packet(), 0)
                        : createDragPacket(drag.packet(), -menu.type().size() + 9);
                adapter.packets().injectClick(player, packet);
            }
        }
        clearAccumulatedDrag(player);
    }

    private ClickPacket createDragPacket(ClickPacket original, int slotOffset) {
        return original.withWindowAndSlot(0, original.slot() + slotOffset, Map.of());
    }

    public void clearAccumulatedDrag(Player player) {
        List<AccumulatedDrag> drags = accumulatedDrag.get(id(player));
        if (drags != null) {
            drags.clear();
        }
    }

    private void resyncDirtySlots(Player player, MenuSession session, ClickPacket packet, UxItem carried) {
        Menu menu = session.menu();
        int last = menu.type().lastIndex();
        int windowId = session.windowId();
        int stateId = session.nextStateId();
        Set<Integer> dirty = new HashSet<>();
        if (packet.slot() >= 0 && packet.slot() <= last) {
            dirty.add(packet.slot());
        }
        for (Integer slot : packet.changedSlots().keySet()) {
            if (slot != null && slot >= 0 && slot <= last) {
                dirty.add(slot);
            }
        }
        List<UxItem> items = menu.items();
        for (Integer slot : dirty) {
            UxItem item = slot < items.size() ? items.get(slot) : UxItem.EMPTY;
            adapter.packets().sendSetSlot(player, windowId, stateId, slot, item);
        }
        adapter.packets().sendSetSlot(player, windowId, stateId, -1, carried == null ? UxItem.EMPTY : carried);
    }

    private ClickPacket syntheticClick(MenuSession session, int slot) {
        return new ClickPacket(
                session.windowId(),
                session.stateId(),
                slot,
                0,
                0,
                WindowClickType.PICKUP,
                Map.of(),
                UxItem.EMPTY
        );
    }

    private ClickPacket createAdjustedClickPacket(ClickPacket packet, Menu menu) {
        int slotOffset = packet.slot() != -999 ? packet.slot() - menu.type().size() + 9 : -999;
        Map<Integer, UxItem> adjustedSlots = new HashMap<>();
        for (Map.Entry<Integer, UxItem> entry : packet.changedSlots().entrySet()) {
            adjustedSlots.put(entry.getKey() - menu.type().size() + 9, entry.getValue());
        }
        return packet.withWindowAndSlot(0, slotOffset, adjustedSlots);
    }

    private void updateCarriedItem(Player player, UxItem carried, ClickType clickType) {
        if (carried == null || adapter.items().isEmpty(carried)) {
            carriedItem.remove(id(player));
            return;
        }
        switch (clickType) {
            case PICKUP, PICKUP_ALL, DRAG_START, DRAG_END -> carriedItem.put(id(player), carried);
            default -> carriedItem.remove(id(player));
        }
    }

    private Menu requireMenu(Player player) {
        return requireSession(player).menu();
    }

    private MenuSession requireSession(Player player) {
        MenuSession session = sessions.get(id(player));
        if (session == null) {
            throw new IllegalStateException("Menu under player key not found.");
        }
        return session;
    }
}
