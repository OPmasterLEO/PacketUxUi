package net.opmasterleo.packetuxui.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.kyori.adventure.text.Component;
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
    private final CopyOnWriteArrayList<Predicate<UxItem>> takeablePredicates = new CopyOnWriteArrayList<>();
    private volatile GuiScopeListener scopeListener;
    private volatile long clickDebounceNanos = 100_000_000L;
    private volatile boolean reclaimCursorOnClose = true;
    private volatile BiConsumer<Player, String> openFailedHandler;

    public MenuService(NmsAdapter adapter, PlatformScheduler scheduler) {
        this.adapter = adapter;
        this.scheduler = scheduler;
    }

    private static UUID id(Player player) {
        return player.getUniqueId();
    }

    public void setScopeListener(GuiScopeListener listener) {
        this.scopeListener = listener;
    }

    public GuiScopeListener scopeListener() {
        return scopeListener;
    }

    public void setClickDebounceMillis(long millis) {
        this.clickDebounceNanos = Math.max(0L, millis) * 1_000_000L;
    }

    public long clickDebounceMillis() {
        return clickDebounceNanos / 1_000_000L;
    }

    public void setReclaimCursorOnClose(boolean reclaim) {
        this.reclaimCursorOnClose = reclaim;
    }

    public void registerTakeablePredicate(Predicate<UxItem> predicate) {
        if (predicate != null) {
            takeablePredicates.add(predicate);
        }
    }

    public void registerTakeablePredicateBukkit(Predicate<ItemStack> predicate) {
        if (predicate == null) {
            return;
        }
        takeablePredicates.add(ux -> {
            if (ux == null || ux.isEmpty()) {
                return false;
            }
            ItemStack stack = adapter.items().toBukkit(ux);
            return stack != null && predicate.test(stack);
        });
    }

    public void clearTakeablePredicates() {
        takeablePredicates.clear();
    }

    public void setOpenFailedHandler(BiConsumer<Player, String> handler) {
        this.openFailedHandler = handler;
    }

    public int getWindowId(Player player) {
        MenuSession session = sessions.get(id(player));
        return session == null ? -1 : session.windowId();
    }

    public int getTopSlotCount(Player player) {
        MenuSession session = sessions.get(id(player));
        return session == null ? 0 : session.topSlotCount();
    }

    public boolean hasOpen(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public SessionPhase phase(Player player) {
        MenuSession session = sessions.get(id(player));
        return session == null ? SessionPhase.IDLE : session.phase();
    }

    public void openMenu(Player player, Menu menu) {
        scheduler.runForPlayer(player, () -> openMenuSync(player, menu));
    }

    public void openMenuSync(Player player, Menu menu) {
        closeCurrent(player, true, false);
        Menu copy = menu.copy();
        int windowId;
        try {
            windowId = windowIds.allocate(player);
        } catch (IllegalStateException exhausted) {
            BiConsumer<Player, String> failed = openFailedHandler;
            if (failed != null) {
                try {
                    failed.accept(player, exhausted.getMessage());
                } catch (Throwable ignored) {
                }
            }
            return;
        }
        MenuSession session = new MenuSession(copy, windowId);
        session.setPhase(SessionPhase.OPENING);
        sessions.put(id(player), session);
        fireScope(player, true, session.topSlotCount());
        int stateId = session.nextStateId();
        adapter.packets().sendOpenWindow(player, windowId, copy.type().id(), copy.name());
        adapter.packets().sendWindowItems(player, windowId, stateId, contentsForOpen(player, copy), null);
        session.setPhase(SessionPhase.OPEN);
    }

    public void present(Player player, Menu menu) {
        scheduler.runForPlayer(player, () -> {
            MenuSession existing = sessions.get(id(player));
            if (existing != null
                    && existing.menu().type() == menu.type()
                    && existing.menu().mode() == menu.mode()
                    && existing.phase() == SessionPhase.OPEN) {
                applyMenuDifferential(player, existing, menu);
                return;
            }
            openMenuSync(player, menu);
        });
    }

    public void updateTitle(Player player, Component title) {
        scheduler.runForPlayer(player, () -> {
            MenuSession session = sessions.get(id(player));
            if (session == null || session.phase() != SessionPhase.OPEN) {
                return;
            }
            Component next = title == null ? Component.empty() : title;
            if (next.equals(session.title())) {
                return;
            }
            session.setTitle(next);
            adapter.packets().sendOpenWindow(
                    player,
                    session.windowId(),
                    session.menu().type().id(),
                    next
            );
            adapter.packets().sendWindowItems(
                    player,
                    session.windowId(),
                    session.nextStateId(),
                    contentsForOpen(player, session.menu()),
                    carriedItem.get(id(player))
            );
        });
    }

    public void onCloseMenu(Player player) {
        closeCurrent(player, false, true);
    }

    public void closeMenu(Player player) {
        scheduler.runForPlayer(player, () -> closeCurrent(player, true, true));
    }

    private void closeCurrent(Player player, boolean sendClosePacket, boolean reclaim) {
        MenuSession session = sessions.get(id(player));
        if (session == null) {
            windowIds.release(player);
            carriedItem.remove(id(player));
            clearAccumulatedDrag(player);
            return;
        }
        session.setPhase(SessionPhase.CLOSING);
        UxItem cursor = carriedItem.getOrDefault(id(player), UxItem.EMPTY);
        BiConsumer<Player, CloseSnapshot> onClose = session.menu().onClose();
        if (onClose != null) {
            try {
                onClose.accept(player, new CloseSnapshot(session.menu().items(), cursor));
            } catch (Throwable ignored) {
            }
        }
        if (reclaim && reclaimCursorOnClose && session.menu().isEditable()) {
            CursorReclaim.reclaim(player, adapter.items(), cursor);
        }
        if (sendClosePacket) {
            adapter.packets().sendCloseWindow(player, session.windowId());
        }
        int top = session.topSlotCount();
        sessions.remove(id(player));
        windowIds.release(player);
        carriedItem.remove(id(player));
        clearAccumulatedDrag(player);
        fireScope(player, false, top);
    }

    private void fireScope(Player player, boolean open, int topSlotCount) {
        GuiScopeListener listener = scopeListener;
        if (listener == null) {
            return;
        }
        try {
            listener.onScopeChanged(player, open, topSlotCount);
        } catch (Throwable ignored) {
        }
    }

    private void applyMenuDifferential(Player player, MenuSession session, Menu next) {
        Menu current = session.menu();
        if (!current.name().equals(next.name())) {
            current.setName(next.name());
            session.setTitle(next.name());
            adapter.packets().sendOpenWindow(player, session.windowId(), next.type().id(), next.name());
        }
        current.buttons().clear();
        current.buttons().putAll(next.buttons());
        List<UxItem> before = current.items();
        List<UxItem> after = next.items();
        current.setItems(after);
        Map<Integer, UxItem> dirty = new HashMap<>();
        int size = next.type().size();
        for (int i = 0; i < size; i++) {
            UxItem a = i < before.size() ? before.get(i) : UxItem.EMPTY;
            UxItem b = i < after.size() ? after.get(i) : UxItem.EMPTY;
            if (!a.equals(b)) {
                dirty.put(i, b);
            }
        }
        if (dirty.isEmpty() && current.name().equals(next.name())) {
            return;
        }
        if (!dirty.isEmpty()) {
            int stateId = session.nextStateId();
            int windowId = session.windowId();
            for (Map.Entry<Integer, UxItem> entry : dirty.entrySet()) {
                adapter.packets().sendSetSlot(player, windowId, stateId, entry.getKey(), entry.getValue());
            }
        } else {
            adapter.packets().sendWindowItems(
                    player,
                    session.windowId(),
                    session.nextStateId(),
                    contentsForOpen(player, current),
                    carriedItem.get(id(player))
            );
        }
        session.bumpGeneration();
    }

    public void handleIncomingClick(Player player, ClickPacket packet) {
        MenuSession session = sessions.get(id(player));
        if (session == null || session.phase() != SessionPhase.OPEN) {
            return;
        }
        long now = System.nanoTime();
        if (clickDebounceNanos > 0L && session.lastClickNanos() > 0L
                && now - session.lastClickNanos() < clickDebounceNanos) {
            if (session.menu().mode() == MenuMode.EDITABLE) {
                rejectEditable(player, session, packet);
            }
            return;
        }
        session.markClick(now);

        ClickData clickData = getClickType(packet);
        if (clickData.clickType() == ClickType.DRAG_START || clickData.clickType() == ClickType.DRAG_ADD) {
            accumulateDrag(player, packet, clickData.clickType());
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
        boolean emptyCursor = packet.carriedEmpty();
        Menu menu = session.menu();
        int last = menu.type().lastIndex();
        boolean touchesTop = (packet.slot() >= 0 && packet.slot() <= last)
                || touchesTopSlots(packet, last);
        if (!touchesTop && emptyCursor && packet.changedSlotIds().isEmpty()) {
            adapter.packets().sendCursorItem(player, UxItem.EMPTY);
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
        if (packet.slot() > last) {
            int idx = packet.slot() - topSize;
            if (idx >= 0 && idx < bottom.size()) {
                adapter.packets().sendSetSlot(player, windowId, stateId, packet.slot(), bottom.get(idx));
            }
        }
        for (Integer slot : packet.changedSlotIds()) {
            if (slot != null && slot > last) {
                int idx = slot - topSize;
                if (idx >= 0 && idx < bottom.size()) {
                    adapter.packets().sendSetSlot(player, windowId, stateId, slot, bottom.get(idx));
                }
            }
        }
        adapter.packets().sendCursorItem(player, UxItem.EMPTY);
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
        if (type == WindowClickType.PICKUP_ALL) {
            rejectEditable(player, session, packet);
            return;
        }

        if (clickData.clickType() == ClickType.DRAG_END) {
            handleEditableDragEnd(player, session, packet);
            return;
        }

        boolean topSlot = packet.slot() >= 0 && packet.slot() <= last;
        if (!topSlot) {
            if (type == WindowClickType.QUICK_MOVE && packet.slot() > last) {
                handleShiftFromBottom(player, session, packet);
                return;
            }
            settleEditableBottom(player, session, packet);
            return;
        }

        SlotKind kind = session.slotKind(packet.slot());
        if (kind == SlotKind.ACTION || kind == SlotKind.DECORATIVE) {
            UxItem at = packet.slot() < menu.items().size() ? menu.items().get(packet.slot()) : UxItem.EMPTY;
            UxItem cursor = carriedItem.getOrDefault(id(player), UxItem.EMPTY);
            boolean taking = cursor.isEmpty() && !at.isEmpty() && isTakeableItem(at);
            if (taking && type == WindowClickType.PICKUP) {
                Predicate<Integer> allow = slot -> slot == packet.slot();
                VirtualClickSimulator.Result result = VirtualClickSimulator.simulate(
                        menu.items(), cursor, packet.slot(), packet.button(), type, allow
                );
                applyEditableResult(player, session, result);
                return;
            }
            if (taking && type == WindowClickType.QUICK_MOVE) {
                VirtualClickSimulator.Result result = VirtualClickSimulator.simulate(
                        menu.items(),
                        cursor,
                        packet.slot(),
                        packet.button(),
                        type,
                        s -> s == packet.slot() || session.isEditableSlot(s)
                );
                applyEditableResult(player, session, result);
                return;
            }
            Button button = menu.buttons().get(packet.slot());
            if (kind == SlotKind.ACTION && button != null) {
                fireLockedButton(player, session, clickData, packet, button);
            } else {
                rejectEditable(player, session, packet);
            }
            return;
        }

        UxItem cursor = carriedItem.getOrDefault(id(player), UxItem.EMPTY);
        VirtualClickSimulator.Result result = VirtualClickSimulator.simulate(
                menu.items(),
                cursor,
                packet.slot(),
                packet.button(),
                type,
                session::isEditableSlot
        );
        applyEditableResult(player, session, result);
    }

    private void handleShiftFromBottom(Player player, MenuSession session, ClickPacket packet) {
        Menu menu = session.menu();
        int topSize = menu.type().size();
        int bottomIndex = packet.slot() - topSize;
        List<UxItem> bottom = snapshotBottom(player);
        if (bottomIndex < 0 || bottomIndex >= bottom.size()) {
            settleEditableBottom(player, session, packet);
            return;
        }
        UxItem moving = bottom.get(bottomIndex);
        if (moving.isEmpty()) {
            settleEditableBottom(player, session, packet);
            return;
        }
        VirtualClickSimulator.Result inserted = VirtualClickSimulator.shiftInto(
                menu.items(),
                moving,
                session::isEditableSlot
        );
        UxItem leftover = inserted.cursor();
        menu.setItems(inserted.items());
        bottom.set(bottomIndex, leftover);
        writeBottom(player, bottom);
        int windowId = session.windowId();
        int stateId = session.nextStateId();
        for (Integer slot : inserted.dirty()) {
            UxItem item = slot < inserted.items().size() ? inserted.items().get(slot) : UxItem.EMPTY;
            adapter.packets().sendSetSlot(player, windowId, stateId, slot, item);
        }
        adapter.packets().sendSetSlot(player, windowId, stateId, packet.slot(), leftover);
        adapter.packets().sendCursorItem(
                player,
                carriedItem.getOrDefault(id(player), UxItem.EMPTY)
        );
    }

    private void settleEditableBottom(Player player, MenuSession session, ClickPacket packet) {
        int topSize = session.menu().type().size();
        int windowId = session.windowId();
        int stateId = session.nextStateId();
        List<UxItem> bottom = snapshotBottom(player);
        Set<Integer> dirty = new HashSet<>();
        if (packet.slot() >= topSize) {
            dirty.add(packet.slot());
        }
        for (Integer slot : packet.changedSlotIds()) {
            if (slot != null && slot >= topSize) {
                dirty.add(slot);
            }
        }
        for (Integer slot : dirty) {
            int idx = slot - topSize;
            if (idx >= 0 && idx < bottom.size()) {
                adapter.packets().sendSetSlot(player, windowId, stateId, slot, bottom.get(idx));
            }
        }
        UxItem cursor = carriedItem.getOrDefault(id(player), UxItem.EMPTY);
        adapter.packets().sendCursorItem(player, cursor);
    }

    private void writeBottom(Player player, List<UxItem> bottom) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 27 && i < bottom.size(); i++) {
            inv.setItem(i + 9, toBukkitOrNull(bottom.get(i)));
        }
        for (int i = 0; i < 9 && (27 + i) < bottom.size(); i++) {
            inv.setItem(i, toBukkitOrNull(bottom.get(27 + i)));
        }
    }

    private ItemStack toBukkitOrNull(UxItem item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        return adapter.items().toBukkit(item);
    }

    private boolean isTakeableItem(UxItem item) {
        if (item == null || item.isEmpty() || takeablePredicates.isEmpty()) {
            return false;
        }
        for (Predicate<UxItem> predicate : takeablePredicates) {
            try {
                if (predicate.test(item)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private void handleEditableDragEnd(Player player, MenuSession session, ClickPacket packet) {
        Menu menu = session.menu();
        List<Integer> slots = new ArrayList<>();
        List<AccumulatedDrag> drags = accumulatedDrag.get(id(player));
        if (drags != null) {
            for (AccumulatedDrag drag : drags) {
                if (drag.type() == ClickType.DRAG_ADD) {
                    slots.add(drag.packet().slot());
                }
            }
        }
        for (Integer slot : packet.changedSlotIds()) {
            if (slot != null) {
                slots.add(slot);
            }
        }
        clearAccumulatedDrag(player);
        if (packet.button() == 10) {
            rejectEditable(player, session, packet);
            return;
        }
        Set<Integer> unique = new HashSet<>(slots);
        boolean allEditable = true;
        for (Integer slot : unique) {
            if (slot == null) {
                continue;
            }
            if (slot < 0 || slot > menu.type().lastIndex() || !session.isEditableSlot(slot)) {
                allEditable = false;
                break;
            }
        }
        if (!allEditable && unique.size() > 1) {
            rejectEditable(player, session, packet);
            for (Integer slot : unique) {
                if (slot != null && slot >= 0 && slot <= menu.type().lastIndex()) {
                    adapter.packets().sendSetSlot(
                            player,
                            session.windowId(),
                            session.stateId(),
                            slot,
                            slot < menu.items().size() ? menu.items().get(slot) : UxItem.EMPTY
                    );
                }
            }
            return;
        }
        if (!allEditable && unique.size() == 1) {
            Integer only = unique.iterator().next();
            if (only == null || !session.isEditableSlot(only)) {
                rejectEditable(player, session, packet);
                return;
            }
        }
        UxItem cursor = carriedItem.getOrDefault(id(player), UxItem.EMPTY);
        VirtualClickSimulator.Result result = VirtualClickSimulator.dragEnd(
                menu.items(),
                cursor,
                slots,
                packet.button(),
                session::isEditableSlot
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
        UUID pid = id(player);
        Menu menu = session.menu();
        boolean dirtyEmpty = result.dirty().isEmpty();
        UxItem nextCursor = result.cursor();
        UxItem prevCursor = carriedItem.getOrDefault(pid, UxItem.EMPTY);
        if (!dirtyEmpty) {
            menu.setItems(result.items());
        }
        if (nextCursor.isEmpty()) {
            carriedItem.remove(pid);
        } else {
            carriedItem.put(pid, nextCursor);
        }
        if (dirtyEmpty && nextCursor.equals(prevCursor)) {
            return;
        }
        int windowId = session.windowId();
        int stateId = session.nextStateId();
        if (!dirtyEmpty) {
            List<UxItem> items = result.items();
            for (Integer slot : result.dirty()) {
                UxItem item = slot < items.size() ? items.get(slot) : UxItem.EMPTY;
                adapter.packets().sendSetSlot(player, windowId, stateId, slot, item);
            }
        }
        adapter.packets().sendCursorItem(player, nextCursor);
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
        adapter.packets().sendCursorItem(player, cursor);
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
        if (stack == null || stack.getAmount() <= 0 || stack.getType() == org.bukkit.Material.AIR) {
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

    public int generation(Player player) {
        MenuSession session = sessions.get(id(player));
        return session == null ? 0 : session.generation();
    }

    public boolean applyIfGeneration(Player player, int expectedGeneration, Runnable apply) {
        MenuSession session = sessions.get(id(player));
        if (session == null || session.phase() != SessionPhase.OPEN || session.generation() != expectedGeneration) {
            return false;
        }
        apply.run();
        return true;
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
                    || touchesTopSlots(packet, last);
            default -> false;
        };
    }

    public ClickData getClickType(ClickPacket packet) {
        return switch (packet.clickType()) {
            case PICKUP -> {
                if (packet.button() == 0) {
                    yield packet.carriedEmpty() ? ClickData.LEFT_PLACE : ClickData.LEFT_PICKUP;
                }
                yield packet.carriedEmpty() ? ClickData.RIGHT_PICKUP : ClickData.RIGHT_PLACE;
            }
            case QUICK_MOVE -> packet.button() == 0 ? ClickData.SHIFT_LEFT : ClickData.SHIFT_RIGHT;
            case SWAP -> {
                int button = packet.button();
                if (button >= 0 && button <= 8) {
                    yield new ClickData(ButtonType.VALUES[9 + button], ClickType.PICKUP);
                }
                if (button == 40) {
                    yield ClickData.F_PICKUP;
                }
                yield ClickData.LEFT_PLACE;
            }
            case CLONE -> ClickData.MIDDLE_PICKUP;
            case THROW -> packet.button() == 0 ? ClickData.DROP_PICKUP : ClickData.CTRL_DROP_PICKUP;
            case QUICK_CRAFT -> switch (packet.button()) {
                case 0 -> ClickData.LEFT_DRAG_START;
                case 4 -> ClickData.RIGHT_DRAG_START;
                case 8 -> ClickData.MIDDLE_DRAG_START;
                case 1 -> ClickData.LEFT_DRAG_ADD;
                case 5 -> ClickData.RIGHT_DRAG_ADD;
                case 9 -> ClickData.MIDDLE_DRAG_ADD;
                case 2 -> ClickData.LEFT_DRAG_END;
                case 6 -> ClickData.RIGHT_DRAG_END;
                case 10 -> ClickData.MIDDLE_DRAG_END;
                default -> ClickData.LEFT_UNDEFINED;
            };
            case PICKUP_ALL -> ClickData.DOUBLE_CLICK;
            default -> ClickData.LEFT_UNDEFINED;
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
        accumulatedDrag.remove(id(player));
    }

    private static boolean touchesTopSlots(ClickPacket packet, int last) {
        for (Integer slot : packet.changedSlotIds()) {
            if (slot != null && slot >= 0 && slot <= last) {
                return true;
            }
        }
        return false;
    }

    private void resyncDirtySlots(Player player, MenuSession session, ClickPacket packet, UxItem carried) {
        Menu menu = session.menu();
        int last = menu.type().lastIndex();
        int windowId = session.windowId();
        int stateId = session.nextStateId();
        if (packet.slot() >= 0 && packet.slot() <= last) {
            List<UxItem> items = menu.items();
            UxItem item = packet.slot() < items.size() ? items.get(packet.slot()) : UxItem.EMPTY;
            adapter.packets().sendSetSlot(player, windowId, stateId, packet.slot(), item);
        }
        List<UxItem> items = menu.items();
        for (Integer slot : packet.changedSlotIds()) {
            if (slot != null && slot >= 0 && slot <= last) {
                UxItem item = slot < items.size() ? items.get(slot) : UxItem.EMPTY;
                adapter.packets().sendSetSlot(player, windowId, stateId, slot, item);
            }
        }
        adapter.packets().sendCursorItem(player, carried == null ? UxItem.EMPTY : carried);
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
        Map<Integer, UxItem> changed = packet.changedSlots();
        if (changed.isEmpty()) {
            return packet.withWindowAndSlot(0, slotOffset, Map.of());
        }
        Map<Integer, UxItem> adjustedSlots = new HashMap<>(changed.size());
        int offset = -menu.type().size() + 9;
        for (Map.Entry<Integer, UxItem> entry : changed.entrySet()) {
            adjustedSlots.put(entry.getKey() + offset, entry.getValue());
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
