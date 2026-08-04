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
import java.util.concurrent.atomic.AtomicLong;
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
    private final ConcurrentHashMap<UUID, EditableBottomMoves.Held> bottomHeld = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<AccumulatedDrag>> accumulatedDrag = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> transitionTokens = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> lastClickDecision = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Predicate<UxItem>> takeablePredicates = new CopyOnWriteArrayList<>();
    private final AtomicLong transitionSequence = new AtomicLong();
    private volatile GuiScopeListener scopeListener;
    private volatile Consumer<ClickDecision> clickDecisionListener;
    private volatile boolean strictActionMode;
    private volatile long clickDebounceNanos = 100_000_000L;
    private volatile boolean reclaimCursorOnClose = true;
    private volatile BiConsumer<Player, String> openFailedHandler;
    private volatile Consumer<Player> pipelineReassert;
    private volatile boolean debugLogging;

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

    public void setClickDecisionListener(Consumer<ClickDecision> listener) {
        this.clickDecisionListener = listener;
    }

    public void setStrictActionMode(boolean strictActionMode) {
        this.strictActionMode = strictActionMode;
    }

    public TransitionToken beginTransition(Player player) {
        long token = transitionSequence.incrementAndGet();
        transitionTokens.put(id(player), token);
        return new TransitionToken(token);
    }

    public boolean endTransition(Player player, TransitionToken token) {
        if (token == null) {
            return false;
        }
        return transitionTokens.remove(id(player), token.value());
    }

    public boolean isTransitionActive(Player player) {
        return transitionTokens.containsKey(id(player));
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

    public void setPipelineReassert(Consumer<Player> reassert) {
        this.pipelineReassert = reassert;
    }

    public void setDebugLogging(boolean enabled) {
        this.debugLogging = enabled;
    }

    public boolean debugLogging() {
        return debugLogging;
    }

    public void debug(Player player, String message) {
        if (!debugLogging) {
            return;
        }
        String who = player == null ? "?" : player.getName();
        System.out.println("[PacketUxUi/debug] " + who + ": " + message);
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
        closeCurrent(player, true, true);
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
        adapter.packets().bindServerContainer(
                player,
                windowId,
                copy.type().id(),
                Math.max(1, copy.type().protocolTopSize() / 9)
        );
        adapter.packets().sendOpenWindow(player, windowId, copy.type().id(), copy.name());
        adapter.packets().sendWindowItems(player, windowId, stateId, fullContents(player, copy), null);
        adapter.packets().sendCursorItem(player, UxItem.EMPTY);
        session.setPhase(SessionPhase.OPEN);
        Consumer<Player> reassert = pipelineReassert;
        if (reassert != null) {
            try {
                reassert.accept(player);
            } catch (Throwable error) {
                debug(player, "pipeline reassert failed: " + error.getClass().getSimpleName());
            }
        }
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

    /** Force close+open even when type/mode match (size/type changes, SignGUI handoff prep, etc.). */
    public void reopen(Player player, Menu menu) {
        openMenu(player, menu);
    }

    /**
     * Fully settles the packet menu (cursor empty, close packet, unbind, session gone), waits
     * {@code settleTicks}, then runs {@code onSettled}. Use this before opening SignGUI or any
     * external UI. Client close packets during the settle window are suppressed via a transition token.
     */
    public void closeThen(Player player, Runnable onSettled) {
        closeThen(player, 1L, onSettled);
    }

    public void closeThen(Player player, long settleTicks, Runnable onSettled) {
        java.util.Objects.requireNonNull(player, "player");
        long delay = Math.max(1L, settleTicks);
        scheduler.runForPlayer(player, () -> {
            TransitionToken token = beginTransition(player);
            try {
                closeCurrent(player, true, true);
            } catch (Throwable error) {
                endTransition(player, token);
                debug(player, "closeThen close failed: " + error.getClass().getSimpleName());
                throw error;
            }
            scheduler.runLaterForPlayer(player, settled -> {
                endTransition(settled, token);
                if (onSettled == null || !settled.isOnline()) {
                    return;
                }
                try {
                    onSettled.run();
                } catch (Throwable error) {
                    debug(settled, "closeThen onSettled failed: " + error.getClass().getSimpleName());
                }
            }, delay);
        });
    }

    public java.util.concurrent.CompletableFuture<Void> closeAsync(Player player) {
        return closeAsync(player, 1L);
    }

    public java.util.concurrent.CompletableFuture<Void> closeAsync(Player player, long settleTicks) {
        java.util.concurrent.CompletableFuture<Void> future = new java.util.concurrent.CompletableFuture<>();
        closeThen(player, settleTicks, () -> future.complete(null));
        return future;
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
        if (isTransitionActive(player)) {
            return;
        }
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
            clearBottomHeld(player);
            clearAccumulatedDrag(player);
            return;
        }
        session.setPhase(SessionPhase.CLOSING);
        UxItem cursor = carriedItem.getOrDefault(id(player), UxItem.EMPTY);
        EditableBottomMoves.Held heldBottom = bottomHeld.get(id(player));
        BiConsumer<Player, CloseSnapshot> onClose = session.menu().onClose();
        if (onClose != null) {
            try {
                onClose.accept(player, new CloseSnapshot(session.menu().items(), cursor));
            } catch (Throwable ignored) {
            }
        }
        if (reclaim && reclaimCursorOnClose) {
            if (cursor != null && !cursor.isEmpty()) {
                CursorReclaim.reclaim(player, adapter.items(), cursor);
            }
            if (heldBottom != null && !heldBottom.isEmpty()) {
                CursorReclaim.reclaim(player, adapter.items(), heldBottom.item());
            }
        }
        // Always clear the client cursor before close. Optimistic pickups from the last click
        // otherwise survive into the player inventory when the virtual window disappears.
        adapter.packets().sendCursorItem(player, UxItem.EMPTY);
        if (sendClosePacket) {
            adapter.packets().sendCloseWindow(player, session.windowId());
        }
        adapter.packets().unbindServerContainer(player);
        int top = session.topSlotCount();
        sessions.remove(id(player));
        windowIds.release(player);
        carriedItem.remove(id(player));
        clearBottomHeld(player);
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
        if (session == null) {
            return;
        }
        if (!isValidClickSlot(session, packet)) {
            emitDecision(player, packet, SlotKind.DECORATIVE, false, false, "invalid_slot_resync_full");
            resyncFull(player, session, packet.stateId(), true);
            return;
        }
        if (session.phase() != SessionPhase.OPEN) {
            emitDecision(player, packet, SlotKind.DECORATIVE, false, false, "phase_mismatch_resync_full");
            resyncFull(player, session, packet.stateId(), true);
            return;
        }
        long now = System.nanoTime();
        if (clickDebounceNanos > 0L && session.lastClickNanos() > 0L
                && now - session.lastClickNanos() < clickDebounceNanos) {
            if (session.menu().mode() == MenuMode.EDITABLE) {
                emitDecision(player, packet, SlotKind.DECORATIVE, false, false, "debounce_reject_editable");
                rejectEditable(player, session, packet);
            } else {
                // Still correct cursor/slots; only the handler is dropped.
                emitDecision(player, packet, SlotKind.DECORATIVE, false, false, "debounce_resync_full");
                resyncFull(player, session, packet.stateId(), true);
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

    private boolean isValidClickSlot(MenuSession session, ClickPacket packet) {
        int slot = packet.slot();
        if (slot == -999 || slot == -1) {
            return true;
        }
        int max = session.menu().type().protocolTopSize() + 36;
        return slot >= 0 && slot < max;
    }

    private void settleReadOnlyOutside(Player player, MenuSession session, ClickPacket packet) {
        Menu menu = session.menu();
        int last = menu.type().protocolLastIndex();
        boolean touchesTop = (packet.slot() >= 0 && packet.slot() <= last)
                || touchesTopSlots(packet, last);
        if (!touchesTop) {
            settleBottomSlots(player, session, packet);
            return;
        }
        resyncDirtySlots(player, session, packet, UxItem.EMPTY);
        carriedItem.remove(id(player));
    }

    private void settleBottomSlots(Player player, MenuSession session, ClickPacket packet) {
        Menu menu = session.menu();
        int topSize = menu.type().protocolTopSize();
        int last = menu.type().protocolLastIndex();
        int maxSlot = topSize + 36;
        List<UxItem> bottom = snapshotBottom(player);
        int windowId = session.windowId();
        int stateId = session.nextStateId();
        Set<Integer> dirty = new HashSet<>();
        if (packet.slot() > last && packet.slot() < maxSlot) {
            dirty.add(packet.slot());
        }
        for (Integer slot : packet.changedSlotIds()) {
            if (slot != null && slot > last && slot < maxSlot) {
                dirty.add(slot);
            }
        }
        for (Integer slot : dirty) {
            int idx = slot - topSize;
            if (idx >= 0 && idx < bottom.size()) {
                adapter.packets().sendSetSlot(player, windowId, stateId, slot, bottom.get(idx));
            }
        }
        adapter.packets().sendCursorItem(player, UxItem.EMPTY);
        carriedItem.remove(id(player));
    }

    private void handleEditableClick(Player player, MenuSession session, ClickData clickData, ClickPacket packet) {
        Menu menu = session.menu();
        int last = menu.type().protocolLastIndex();
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
            if (packet.slot() == -999) {
                handleEditableOutsideClick(player, session);
                return;
            }
            if (type == WindowClickType.QUICK_MOVE && packet.slot() > last) {
                if (bottomHeld.containsKey(id(player))) {
                    restoreBottomHeld(player, session);
                }
                handleShiftFromBottom(player, session, packet);
                return;
            }
            if (type == WindowClickType.PICKUP) {
                handleEditableBottomPickup(player, session, packet);
                return;
            }
            settleEditableBottom(player, session, packet);
            return;
        }

        if (bottomHeld.containsKey(id(player))) {
            restoreBottomHeld(player, session);
        }

        SlotKind kind = session.slotKind(packet.slot());
        if (kind == SlotKind.ACTION || kind == SlotKind.DECORATIVE) {
            UxItem at = packet.slot() < menu.items().size() ? menu.items().get(packet.slot()) : UxItem.EMPTY;
            UxItem cursor = carriedItem.getOrDefault(id(player), UxItem.EMPTY);
            boolean taking = cursor.isEmpty() && !at.isEmpty() && isTakeableItem(at);
            if (strictActionMode && kind == SlotKind.ACTION) {
                taking = false;
            }
            if (taking && type == WindowClickType.PICKUP) {
                Predicate<Integer> allow = slot -> slot == packet.slot();
                VirtualClickSimulator.Result result = VirtualClickSimulator.simulate(
                        menu.items(), cursor, packet.slot(), packet.button(), type, allow
                );
                emitDecision(player, packet, kind, false, true, "takeable_pickup_simulate");
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
                emitDecision(player, packet, kind, false, true, "takeable_shift_simulate");
                applyEditableResult(player, session, result);
                return;
            }
            Button button = menu.buttons().get(packet.slot());
            if (kind == SlotKind.ACTION && button != null) {
                emitDecision(player, packet, kind, true, false, "action_execute");
                fireLockedButton(player, session, clickData, packet, button);
            } else {
                emitDecision(player, packet, kind, button != null, false, "action_reject");
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
        emitDecision(player, packet, kind, false, false, "editable_simulate");
        applyEditableResult(player, session, result);
    }

    private void handleShiftFromBottom(Player player, MenuSession session, ClickPacket packet) {
        Menu menu = session.menu();
        int topSize = menu.type().protocolTopSize();
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
        int topSize = session.menu().type().protocolTopSize();
        int maxSlot = topSize + 36;
        int windowId = session.windowId();
        int stateId = session.nextStateId();
        List<UxItem> bottom = snapshotBottom(player);
        Set<Integer> dirty = new HashSet<>();
        if (packet.slot() >= topSize && packet.slot() < maxSlot) {
            dirty.add(packet.slot());
        }
        for (Integer slot : packet.changedSlotIds()) {
            if (slot != null && slot >= topSize && slot < maxSlot) {
                dirty.add(slot);
            }
        }
        for (Integer slot : dirty) {
            int idx = slot - topSize;
            if (idx >= 0 && idx < bottom.size()) {
                adapter.packets().sendSetSlot(player, windowId, stateId, slot, bottom.get(idx));
            }
        }
        adapter.packets().sendCursorItem(player, activeCursor(player));
    }

    private void handleEditableBottomPickup(Player player, MenuSession session, ClickPacket packet) {
        UUID pid = id(player);
        if (!carriedItem.getOrDefault(pid, UxItem.EMPTY).isEmpty()) {
            settleEditableBottom(player, session, packet);
            return;
        }
        int topSize = session.menu().type().protocolTopSize();
        int maxSlot = topSize + 36;
        int slot = packet.slot();
        if (slot < topSize || slot >= maxSlot) {
            settleEditableBottom(player, session, packet);
            return;
        }
        int bottomIndex = slot - topSize;
        List<UxItem> bottom = snapshotBottom(player);
        EditableBottomMoves.Held previous = bottomHeld.get(pid);
        EditableBottomMoves.Outcome outcome = EditableBottomMoves.applyPickup(
                bottom,
                previous,
                bottomIndex,
                packet.button()
        );
        writeBottom(player, outcome.bottom());
        if (outcome.held() == null || outcome.held().isEmpty()) {
            bottomHeld.remove(pid);
        } else {
            bottomHeld.put(pid, outcome.held());
        }
        int windowId = session.windowId();
        int stateId = session.nextStateId();
        for (Integer dirty : outcome.dirty()) {
            if (dirty == null || dirty < 0 || dirty >= outcome.bottom().size()) {
                continue;
            }
            adapter.packets().sendSetSlot(
                    player,
                    windowId,
                    stateId,
                    topSize + dirty,
                    outcome.bottom().get(dirty)
            );
        }
        UxItem cursor = outcome.held() == null ? UxItem.EMPTY : outcome.held().item();
        adapter.packets().sendCursorItem(player, cursor);
    }

    private void handleEditableOutsideClick(Player player, MenuSession session) {
        UUID pid = id(player);
        EditableBottomMoves.Held held = bottomHeld.remove(pid);
        if (held != null && !held.isEmpty()) {
            ItemStack stack = adapter.items().toBukkit(held.item());
            if (stack != null && !stack.getType().isAir() && stack.getAmount() > 0 && player.getWorld() != null) {
                player.getWorld().dropItemNaturally(player.getLocation(), stack);
            }
        }
        UxItem topCursor = carriedItem.getOrDefault(pid, UxItem.EMPTY);
        if (!topCursor.isEmpty()) {
            rejectEditable(player, session, syntheticClick(session, -999));
            return;
        }
        adapter.packets().sendCursorItem(player, UxItem.EMPTY);
    }

    private void restoreBottomHeld(Player player, MenuSession session) {
        UUID pid = id(player);
        EditableBottomMoves.Held held = bottomHeld.remove(pid);
        if (held == null || held.isEmpty()) {
            adapter.packets().sendCursorItem(player, carriedItem.getOrDefault(pid, UxItem.EMPTY));
            return;
        }
        List<UxItem> before = snapshotBottom(player);
        List<UxItem> restored = EditableBottomMoves.returnToOrigin(before, held);
        if (!EditableBottomMoves.fullyReturned(before, restored, held)) {
            CursorReclaim.reclaim(player, adapter.items(), held.item());
            restored = snapshotBottom(player);
        } else {
            writeBottom(player, restored);
        }
        int topSize = session.menu().type().protocolTopSize();
        int windowId = session.windowId();
        int stateId = session.nextStateId();
        int origin = held.originIndex();
        if (origin >= 0 && origin < restored.size()) {
            adapter.packets().sendSetSlot(player, windowId, stateId, topSize + origin, restored.get(origin));
        }
        adapter.packets().sendCursorItem(player, UxItem.EMPTY);
    }

    private void clearBottomHeld(Player player) {
        bottomHeld.remove(id(player));
    }

    private UxItem activeCursor(Player player) {
        UUID pid = id(player);
        EditableBottomMoves.Held held = bottomHeld.get(pid);
        if (held != null && !held.isEmpty()) {
            return held.item();
        }
        return carriedItem.getOrDefault(pid, UxItem.EMPTY);
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
            if (slot < 0 || slot > menu.type().protocolLastIndex() || !session.isEditableSlot(slot)) {
                allEditable = false;
                break;
            }
        }
        if (!allEditable && unique.size() > 1) {
            rejectEditable(player, session, packet);
            for (Integer slot : unique) {
                if (slot != null && slot >= 0 && slot <= menu.type().protocolLastIndex()) {
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
        int last = menu.type().protocolLastIndex();
        int topSize = menu.type().protocolTopSize();
        int maxSlot = topSize + 36;
        int windowId = session.windowId();
        int stateId = session.nextStateId();
        UxItem cursor = activeCursor(player);
        List<UxItem> bottom = null;
        if (packet.slot() >= 0 && packet.slot() <= last) {
            UxItem item = packet.slot() < menu.items().size() ? menu.items().get(packet.slot()) : UxItem.EMPTY;
            adapter.packets().sendSetSlot(player, windowId, stateId, packet.slot(), item);
        }
        if (packet.slot() >= topSize && packet.slot() < maxSlot) {
            bottom = snapshotBottom(player);
            int idx = packet.slot() - topSize;
            if (idx >= 0 && idx < bottom.size()) {
                adapter.packets().sendSetSlot(player, windowId, stateId, packet.slot(), bottom.get(idx));
            }
        }
        for (Integer slot : packet.changedSlotIds()) {
            if (slot == null) {
                continue;
            }
            if (slot >= 0 && slot <= last) {
                UxItem item = slot < menu.items().size() ? menu.items().get(slot) : UxItem.EMPTY;
                adapter.packets().sendSetSlot(player, windowId, stateId, slot, item);
            } else if (slot >= topSize && slot < maxSlot) {
                if (bottom == null) {
                    bottom = snapshotBottom(player);
                }
                int idx = slot - topSize;
                if (idx >= 0 && idx < bottom.size()) {
                    adapter.packets().sendSetSlot(player, windowId, stateId, slot, bottom.get(idx));
                }
            }
        }
        adapter.packets().sendCursorItem(player, cursor);
    }

    private void resyncFull(Player player, MenuSession session) {
        resyncFull(player, session, -1, true);
    }

    private void resyncFull(Player player, MenuSession session, int clientStateId, boolean clearCursor) {
        int stateId = clientStateId >= 0 ? session.nextStateIdAbove(clientStateId) : session.nextStateId();
        if (clearCursor) {
            carriedItem.remove(id(player));
            clearBottomHeld(player);
        }
        UxItem cursor = clearCursor ? UxItem.EMPTY : activeCursor(player);
        adapter.packets().sendWindowItems(
                player,
                session.windowId(),
                stateId,
                fullContents(player, session.menu()),
                cursor.isEmpty() ? null : cursor
        );
        if (clearCursor) {
            adapter.packets().sendCursorItem(player, UxItem.EMPTY);
        }
    }

    /**
     * Netty-thread-safe correction for read-only menus. Only touches in-memory menu state and
     * outbound packets — never Bukkit inventory APIs (unsafe off the region/main thread).
     */
    public void correctReadOnlyClick(Player player, ClickPacket packet) {
        MenuSession session = sessions.get(id(player));
        if (session == null || session.phase() != SessionPhase.OPEN) {
            return;
        }
        if (session.menu().mode() != MenuMode.READ_ONLY) {
            return;
        }
        int stateId = session.nextStateIdAbove(packet.stateId());
        int windowId = session.windowId();
        int top = session.menu().type().protocolTopSize();
        List<UxItem> items = session.menu().items();

        int slot = packet.slot();
        if (slot >= 0 && slot < top) {
            UxItem item = slot < items.size() ? items.get(slot) : UxItem.EMPTY;
            adapter.packets().sendSetSlot(player, windowId, stateId, slot, item == null ? UxItem.EMPTY : item);
        }
        for (Integer changed : packet.changedSlotIds()) {
            if (changed == null || changed == slot || changed < 0 || changed >= top) {
                continue;
            }
            UxItem item = changed < items.size() ? items.get(changed) : UxItem.EMPTY;
            adapter.packets().sendSetSlot(player, windowId, stateId, changed, item == null ? UxItem.EMPTY : item);
        }
        adapter.packets().sendCursorItem(player, UxItem.EMPTY);
        carriedItem.remove(id(player));
    }

    private List<UxItem> contentsForOpen(Player player, Menu menu) {
        return fullContents(player, menu);
    }

    private List<UxItem> fullContents(Player player, Menu menu) {
        int top = menu.type().protocolTopSize();
        List<UxItem> contents = new ArrayList<>(top + 36);
        List<UxItem> items = menu.items();
        for (int i = 0; i < top; i++) {
            contents.add(i < items.size() ? items.get(i) : UxItem.EMPTY);
        }
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
        if (!canInjectPlayerInventoryClick(packet, menu)) {
            settleEditableBottom(player, session, packet);
            return;
        }
        ClickData clickData = getClickType(packet);
        updateCarriedItem(player, packet.carried(), clickData.clickType());
        if (clickData.clickType() == ClickType.DRAG_END) {
            handleDragEnd(player, menu);
        }
        adapter.packets().injectClick(player, createAdjustedClickPacket(packet, menu));
    }

    private boolean canInjectPlayerInventoryClick(ClickPacket packet, Menu menu) {
        int slot = packet.slot();
        if (slot == -999) {
            return true;
        }
        int remapped = slot - menu.type().protocolTopSize() + 9;
        return remapped >= 0 && remapped <= 45;
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
        // Full resync beats client optimistic pickup + stateId races on read-only packet menus.
        resyncFull(player, session, packet.stateId(), true);
        if (button == null) {
            emitDecision(player, packet, SlotKind.DECORATIVE, false, false, "readonly_no_handler");
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
            emitDecision(player, packet, SlotKind.ACTION, true, false, "readonly_execute");
            execute.accept(new ExecuteComponent(player, clickData.buttonType(), slot, UxItem.EMPTY));
        } else {
            emitDecision(player, packet, SlotKind.ACTION, true, false, "readonly_cooldown_block");
        }
    }

    public void refreshWindow(Player player) {
        scheduler.runForPlayer(player, () -> {
            MenuSession session = sessions.get(id(player));
            if (session == null) {
                return;
            }
            boolean editable = session.menu().mode() == MenuMode.EDITABLE;
            if (!editable) {
                carriedItem.remove(id(player));
            }
            UxItem cursor = editable
                    ? carriedItem.getOrDefault(id(player), UxItem.EMPTY)
                    : UxItem.EMPTY;
            adapter.packets().sendWindowItems(
                    player,
                    session.windowId(),
                    session.nextStateId(),
                    fullContents(player, session.menu()),
                    cursor.isEmpty() ? null : cursor
            );
            if (!editable) {
                adapter.packets().sendCursorItem(player, UxItem.EMPTY);
            }
        });
    }

    public void updateItem(Player player, UxItem item, int slot) {
        scheduler.runForPlayer(player, () -> {
            MenuSession session = sessions.get(id(player));
            if (session == null) {
                return;
            }
            Menu menu = session.menu();
            if (slot < 0 || slot > menu.type().lastIndex()) {
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
                if (slot == null || slot < 0 || slot > menu.type().lastIndex()) {
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
            if (slot < 0 || slot > menu.type().lastIndex()) {
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
                if (slot == null || slot < 0 || slot > menu.type().lastIndex()) {
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

    public void updateButtonsBySlot(Player player, Map<Integer, Button> patches) {
        scheduler.runForPlayer(player, () -> {
            MenuSession session = sessions.get(id(player));
            if (session == null || patches == null || patches.isEmpty()) {
                return;
            }
            Menu menu = session.menu();
            for (Integer slot : patches.keySet()) {
                if (slot == null || slot < 0 || slot > menu.type().lastIndex()) {
                    throw new IllegalArgumentException("Slot out of range.");
                }
            }
            List<UxItem> items = new ArrayList<>(menu.items());
            Map<Integer, UxItem> dirty = new HashMap<>();
            for (Map.Entry<Integer, Button> entry : patches.entrySet()) {
                int slot = entry.getKey();
                Button button = entry.getValue();
                if (button == null) {
                    menu.buttons().remove(slot);
                    UxItem current = slot < items.size() ? items.get(slot) : UxItem.EMPTY;
                    if (!current.isEmpty()) {
                        items.set(slot, UxItem.EMPTY);
                        dirty.put(slot, UxItem.EMPTY);
                    }
                    continue;
                }
                menu.buttons().put(slot, button);
                UxItem next = button.item();
                UxItem current = slot < items.size() ? items.get(slot) : UxItem.EMPTY;
                if (!current.equals(next)) {
                    items.set(slot, next);
                    dirty.put(slot, next);
                }
            }
            menu.setItems(items);
            if (dirty.isEmpty()) {
                return;
            }
            int stateId = session.nextStateId();
            for (Map.Entry<Integer, UxItem> entry : dirty.entrySet()) {
                adapter.packets().sendSetSlot(player, session.windowId(), stateId, entry.getKey(), entry.getValue());
            }
        });
    }

    public void clearButtons(Player player, Set<Integer> slots) {
        if (slots == null || slots.isEmpty()) {
            return;
        }
        Map<Integer, Button> patches = new HashMap<>();
        for (Integer slot : slots) {
            patches.put(slot, null);
        }
        updateButtonsBySlot(player, patches);
    }

    public void patchSlotAtomic(Player player, int slot, UxItem item, Consumer<IButtonBuilder> buttonBuilder, SlotKind slotKind) {
        scheduler.runForPlayer(player, () -> {
            MenuSession session = sessions.get(id(player));
            if (session == null) {
                return;
            }
            Menu menu = session.menu();
            if (slot < 0 || slot > menu.type().lastIndex()) {
                throw new IllegalArgumentException("Slot out of range.");
            }
            Button button;
            if (buttonBuilder == null) {
                button = new Button(item == null ? UxItem.EMPTY : item, null, new CooldownComponent(), slotKind);
            } else {
                ButtonBuilder builder = new ButtonBuilder();
                buttonBuilder.accept(builder);
                button = builder.kind(slotKind == null ? SlotKind.ACTION : slotKind).item(item == null ? UxItem.EMPTY : item).build();
            }
            menu.buttons().put(slot, button);
            List<UxItem> copy = new ArrayList<>(menu.items());
            UxItem next = button.item();
            UxItem current = slot < copy.size() ? copy.get(slot) : UxItem.EMPTY;
            if (current.equals(next)) {
                return;
            }
            copy.set(slot, next);
            menu.setItems(copy);
            adapter.packets().sendSetSlot(player, session.windowId(), session.nextStateId(), slot, next);
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

    public MenuSessionDiagnostics diagnostics(Player player) {
        UUID playerId = id(player);
        MenuSession session = sessions.get(playerId);
        if (session == null) {
            return new MenuSessionDiagnostics(
                    playerId,
                    0,
                    SessionPhase.IDLE,
                    -1,
                    Component.empty(),
                    transitionTokens.containsKey(playerId),
                    lastClickDecision.getOrDefault(playerId, "none")
            );
        }
        return new MenuSessionDiagnostics(
                playerId,
                session.generation(),
                session.phase(),
                session.windowId(),
                session.title(),
                transitionTokens.containsKey(playerId),
                lastClickDecision.getOrDefault(playerId, "none")
        );
    }

    public boolean shouldIgnore(int id, Player player) {
        MenuSession session = sessions.get(id(player));
        return session == null || session.windowId() != id;
    }

    public boolean isMenuClick(ClickPacket packet, ClickType clickType, Player player) {
        Menu menu = requireMenu(player);
        int last = menu.type().protocolLastIndex();
        boolean topSlot = packet.slot() >= 0 && packet.slot() <= last;
        return switch (clickType) {
            case SHIFT_CLICK, PICKUP, PLACE -> topSlot;
            case DRAG_END, PICKUP_ALL -> topSlot || touchesTopSlots(packet, last);
            default -> topSlot;
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
                        : createDragPacket(drag.packet(), -menu.type().protocolTopSize() + 9);
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

    private static boolean touchesBottomSlots(ClickPacket packet, int topSize) {
        if (packet.slot() >= topSize) {
            return true;
        }
        for (Integer slot : packet.changedSlotIds()) {
            if (slot != null && slot >= topSize) {
                return true;
            }
        }
        return false;
    }

    private void emitDecision(Player player, ClickPacket packet, SlotKind slotKind, boolean handlerFound, boolean takeable, String result) {
        UUID playerId = id(player);
        lastClickDecision.put(playerId, result);
        Consumer<ClickDecision> listener = clickDecisionListener;
        if (listener == null) {
            return;
        }
        try {
            listener.accept(new ClickDecision(
                    playerId,
                    packet.windowId(),
                    packet.slot(),
                    slotKind,
                    handlerFound,
                    takeable,
                    result
            ));
        } catch (Throwable ignored) {
        }
    }

    private void resyncDirtySlots(Player player, MenuSession session, ClickPacket packet, UxItem carried) {
        Menu menu = session.menu();
        int last = menu.type().protocolLastIndex();
        int windowId = session.windowId();
        int stateId = session.nextStateId();
        if (packet.slot() >= 0 && packet.slot() <= last) {
            List<UxItem> items = menu.items();
            UxItem item = packet.slot() < items.size() ? menu.items().get(packet.slot()) : UxItem.EMPTY;
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
        if (packet.slot() == -999) {
            return packet.withWindowAndSlot(0, -999, Map.of());
        }
        int offset = -menu.type().protocolTopSize() + 9;
        int slotOffset = packet.slot() + offset;
        Map<Integer, UxItem> changed = packet.changedSlots();
        if (changed.isEmpty()) {
            return packet.withWindowAndSlot(0, slotOffset, Map.of());
        }
        Map<Integer, UxItem> adjustedSlots = new HashMap<>(changed.size());
        for (Map.Entry<Integer, UxItem> entry : changed.entrySet()) {
            int adjusted = entry.getKey() + offset;
            if (adjusted < 0 || adjusted > 45) {
                continue;
            }
            adjustedSlots.put(adjusted, entry.getValue());
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
