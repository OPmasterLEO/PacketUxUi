package net.opmasterleo.packetuxui.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.dto.AccumulatedDrag;
import net.opmasterleo.packetuxui.dto.CooldownComponent;
import net.opmasterleo.packetuxui.event.GuiClickEvent;
import net.opmasterleo.packetuxui.event.GuiClickPostEvent;
import net.opmasterleo.packetuxui.event.GuiCloseEvent;
import net.opmasterleo.packetuxui.event.GuiCloseReason;
import net.opmasterleo.packetuxui.event.GuiDragEvent;
import net.opmasterleo.packetuxui.event.GuiDragPhase;
import net.opmasterleo.packetuxui.event.GuiEventManager;
import net.opmasterleo.packetuxui.event.GuiOpenEvent;
import net.opmasterleo.packetuxui.event.GuiOpenReason;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.LiveLimits;
import net.opmasterleo.packetuxui.nms.NmsAdapter;
import net.opmasterleo.packetuxui.nms.WindowClickType;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.types.ButtonType;
import net.opmasterleo.packetuxui.types.ClickData;
import net.opmasterleo.packetuxui.types.ClickType;
import net.opmasterleo.packetuxui.types.ExecuteComponent;
import net.opmasterleo.packetuxui.util.Predicates;

public final class MenuService {

    /** @deprecated Per-player vanilla window ids; do not hardcode. */
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
    private final ConcurrentHashMap<UUID, Integer> nettyRoCorrectedForState = new ConcurrentHashMap<>();
    /** Netty-safe bottom inventory snapshot (no Bukkit touch on event-loop). */
    private final ConcurrentHashMap<UUID, List<UxItem>> bottomCache = new ConcurrentHashMap<>();
    /** Per-player close re-entrancy guard (button onClose → present must not nest-corrupt). */
    private final java.util.Set<UUID> closingPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** Open requested while {@link #closingPlayers} — applied after close finally. */
    private final ConcurrentHashMap<UUID, Menu> pendingPresent = new ConcurrentHashMap<>();
    /**
     * After a silent type swap, the client often echoes {@code ServerboundContainerClosePacket}
     * for the previous screen. Ignore those while the session stays OPEN for a short grace.
     */
    private final ConcurrentHashMap<UUID, Long> ignoreInboundCloseUntilNanos = new ConcurrentHashMap<>();
    private static final long TYPE_SWAP_CLOSE_GRACE_NANOS = 500_000_000L; // 500ms
    /** Soft book-viewer tracking (client book screen has no container close packet). */
    private final java.util.Set<UUID> bookViewers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, java.util.function.Consumer<Player>> bookOnClose =
            new ConcurrentHashMap<>();
    private final AtomicLong transitionSequence = new AtomicLong();
    private final GuiEventManager events = new GuiEventManager();
    private volatile GuiScopeListener scopeListener;
    private volatile long clickDebounceNanos = 100_000_000L;
    private volatile boolean reclaimCursorOnClose = true;
    private volatile BiConsumer<Player, String> openFailedHandler;
    private volatile Consumer<Player> pipelineReassert;
    private volatile boolean debugLogging;

    private static final Function<UUID, List<AccumulatedDrag>> NEW_DRAG_LIST = new NewDragList();

    public MenuService(NmsAdapter adapter, PlatformScheduler scheduler) {
        this.adapter = adapter;
        this.scheduler = scheduler;
    }

    public GuiEventManager events() {
        return events;
    }

    private static UUID id(Player player) {
        return player.getUniqueId();
    }

    public void setScopeListener(GuiScopeListener listener) {
        this.scopeListener = listener;
    }

    TransitionToken beginTransition(Player player) {
        long token = transitionSequence.incrementAndGet();
        transitionTokens.put(id(player), token);
        return new TransitionToken(token);
    }

    boolean endTransition(Player player, TransitionToken token) {
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
        takeablePredicates.add(new BukkitTakeablePredicate(this, predicate));
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
        String who;
        if (player == null) {
            who = "?";
        } else if (scheduler.isOwnedByCurrentRegion(player)) {
            who = player.getName();
        } else {
            // Netty / async — avoid Folia player name access off the entity thread.
            who = player.getUniqueId().toString();
        }
        String line = "[PacketUxUi/debug] " + who + ": " + message;
        try {
            org.bukkit.plugin.java.JavaPlugin host = net.opmasterleo.packetuxui.PacketUxUiAPI.host();
            if (host != null) {
                host.getLogger().info(line);
                return;
            }
        } catch (Throwable ignored) {
        }
        System.out.println(line);
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
        MenuSession session = sessions.get(playerId);
        return session != null && session.phase() == SessionPhase.OPEN;
    }

    public boolean hasOpen(Player player) {
        return player != null && hasOpen(player.getUniqueId());
    }

    /** Any in-memory session (OPEN/OPENING/CLOSING) — used for packet intercept. */
    public boolean hasSession(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public boolean isClosing(UUID playerId) {
        return closingPlayers.contains(playerId);
    }

    public boolean isOursWindow(UUID playerId, int windowId) {
        return windowIds.isOurs(playerId, windowId);
    }

    /**
     * Drop stale client CloseWindow after {@link #replaceMenuInPlace} while the virtual
     * session is still OPEN. Real Esc after the grace still closes normally.
     */
    public boolean shouldIgnoreInboundClose(UUID playerId) {
        Long until = ignoreInboundCloseUntilNanos.get(playerId);
        if (until == null) {
            return false;
        }
        if (System.nanoTime() > until) {
            ignoreInboundCloseUntilNanos.remove(playerId, until);
            return false;
        }
        MenuSession session = sessions.get(playerId);
        return session != null && session.phase() == SessionPhase.OPEN;
    }

    private void armTypeSwapCloseIgnore(UUID playerId) {
        ignoreInboundCloseUntilNanos.put(playerId, System.nanoTime() + TYPE_SWAP_CLOSE_GRACE_NANOS);
    }

    /**
     * Hard-reset all menu tracking for a player (stale session, stranded transition, etc.).
     * Safe to call from join/quit or after a desync.
     */
    public void resetPlayer(Player player) {
        if (player == null) {
            return;
        }
        UUID pid = id(player);
        pendingPresent.remove(pid);
        closingPlayers.add(pid);
        try {
            MenuSession session = sessions.get(pid);
            if (session != null) {
                try {
                    adapter.packets().sendCursorItem(player, UxItem.EMPTY);
                } catch (Throwable ignored) {
                }
                try {
                    adapter.packets().sendCloseWindow(player, session.windowId());
                } catch (Throwable ignored) {
                }
                try {
                    adapter.packets().unbindServerContainer(player);
                } catch (Throwable ignored) {
                }
            }
            clearPlayerTracking(player, true);
            transitionTokens.remove(pid);
            pendingPresent.remove(pid);
            endBookView(player, false);
        } finally {
            closingPlayers.remove(pid);
        }
    }

    /** Quit/kick/death: fire close then purge every tracking map. */
    public void onDisconnect(Player player, GuiCloseReason reason) {
        if (player == null) {
            return;
        }
        UUID pid = id(player);
        pendingPresent.remove(pid);
        try {
            onCloseMenu(player, reason == null ? GuiCloseReason.QUIT : reason);
        } catch (Throwable ignored) {
        } finally {
            transitionTokens.remove(pid);
            pendingPresent.remove(pid);
            closingPlayers.remove(pid);
            endBookView(player, true);
            if (sessions.containsKey(pid)) {
                clearPlayerTracking(player, true);
            }
        }
    }

    public SessionPhase phase(Player player) {
        MenuSession session = sessions.get(id(player));
        return session == null ? SessionPhase.IDLE : session.phase();
    }

    public void openMenu(Player player, Menu menu) {
        scheduler.runForPlayer(player, new OpenMenuTask(this, player, menu));
    }

    public void openMenuSync(Player player, Menu menu) {
        UUID pid = id(player);
        if (closingPlayers.contains(pid)) {
            pendingPresent.put(pid, menu);
            return;
        }
        closeCurrent(player, true, true, GuiCloseReason.REPLACE, true);
        if (closingPlayers.contains(pid)) {
            pendingPresent.put(pid, menu);
            return;
        }
        installOpen(player, menu);
    }

    public void present(Player player, Menu menu) {
        scheduler.runForPlayer(player, new PresentTask(this, player, menu));
    }

    /**
     * Swap menu type/size/mode (hopper↔chest, 27↔54, READ_ONLY↔EDITABLE, …) without
     * CloseWindow or session teardown. Same window id; OpenScreen + SetContent only.
     * Cursor preserved when staying EDITABLE. No {@code onClose} / GuiCloseEvent.
     */
    private void replaceMenuInPlace(Player player, MenuSession existing, Menu menu) {
        Menu copy = menu.copy();
        int windowId = existing.windowId();
        boolean stayEditable = existing.menu().mode() == MenuMode.EDITABLE
                && copy.mode() == MenuMode.EDITABLE;
        UxItem cursor = stayEditable ? activeCursor(player) : UxItem.EMPTY;
        if (!stayEditable) {
            carriedItem.remove(id(player));
            clearBottomHeld(player);
        }

        existing.replaceMenu(copy);
        existing.setPhase(SessionPhase.OPEN);
        clearAccumulatedDrag(player);
        nettyRoCorrectedForState.remove(id(player));
        // Client often echoes CloseWindow when OpenScreen changes type — do not tear down.
        armTypeSwapCloseIgnore(id(player));

        adapter.packets().unbindServerContainer(player);
        adapter.items().preload(copy.items());

        boolean bound = false;
        if (copy.type().supportsChestBind()) {
            adapter.packets().bindServerContainer(
                    player,
                    windowId,
                    copy.type().id(),
                    Math.max(1, copy.type().protocolTopSize() / 9)
            );
            adapter.packets().mirrorTopSlots(player, copy.items());
            bound = adapter.packets().ownsBoundContainer(player);
        }

        int stateId = protocolState(player, existing, 0);
        // Protocol requires OpenScreen to change container type — no CloseWindow before it.
        adapter.packets().sendOpenWindow(player, windowId, copy.type().id(), copy.name());

        List<UxItem> bottom = bottomCache.get(id(player));
        if (bottom == null || bottom.size() != copy.type().bottomSlotCount()) {
            bottom = snapshotBottom(player);
            if (copy.type().bottomSlotCount() > 0) {
                bottomCache.put(id(player), bottom);
            }
        }
        if (bound && adapter.packets().sendBoundAuthority(player, stateId, !stayEditable)) {
            if (stayEditable && cursor != null && !cursor.isEmpty()) {
                adapter.packets().sendCursorItem(player, cursor);
            }
        } else {
            List<UxItem> contents = assembleContents(copy, bottom);
            adapter.packets().sendWindowItems(
                    player,
                    windowId,
                    stateId,
                    contents,
                    stayEditable ? cursor : null
            );
            if (!stayEditable) {
                adapter.packets().sendCursorItem(player, UxItem.EMPTY);
            } else if (cursor != null && !cursor.isEmpty()) {
                adapter.packets().sendCursorItem(player, cursor);
            }
        }

        ensurePipeline(player);
        if (debugLogging) {
            debug(player, "TYPE_SWAP windowId=" + windowId
                    + " type=" + copy.type()
                    + " mode=" + copy.mode()
                    + " stateId=" + stateId
                    + " bound=" + bound
                    + " cursorKept=" + stayEditable);
        }
        // OpenScreen shown — InventoryOpen analogue only (no close / no scope flip).
        fireOpenEvent(player, existing, GuiOpenReason.TYPE_SWAP);
    }

    /** Bind + OpenScreen + contents for a freshly allocated or reused window id. */
    private void installOpen(Player player, Menu menu) {
        endBookView(player, true);
        Menu copy = menu.copy();
        int windowId = windowIds.allocate(player, new AllocateWindowId(this, player));
        MenuSession session = new MenuSession(copy, windowId);
        session.setPhase(SessionPhase.OPENING);
        sessions.put(id(player), session);
        fireScope(player, true, session, session.topSlotCount(), GuiCloseReason.UNKNOWN, null);
        adapter.items().preload(copy.items());
        boolean bound = false;
        if (copy.type().supportsChestBind()) {
            adapter.packets().bindServerContainer(
                    player,
                    windowId,
                    copy.type().id(),
                    Math.max(1, copy.type().protocolTopSize() / 9)
            );
            adapter.packets().mirrorTopSlots(player, copy.items());
            bound = adapter.packets().ownsBoundContainer(player);
        }
        int stateId = protocolState(player, session, 0);
        adapter.packets().sendOpenWindow(player, windowId, copy.type().id(), copy.name());
        if (bound && adapter.packets().sendBoundAuthority(player, stateId, true)) {
            // Direct NMS slots.
        } else {
            List<UxItem> bottom = snapshotBottom(player);
            bottomCache.put(id(player), bottom);
            List<UxItem> contents = assembleContents(copy, bottom);
            adapter.packets().sendWindowItems(player, windowId, stateId, contents, null);
        }
        adapter.packets().sendCursorItem(player, UxItem.EMPTY);
        session.setPhase(SessionPhase.OPEN);
        if (debugLogging) {
            debug(player, "OPEN windowId=" + windowId
                    + " type=" + copy.type()
                    + " mode=" + copy.mode()
                    + " stateId=" + stateId
                    + " top=" + session.topSlotCount()
                    + " bound=" + bound);
        }
        ensurePipeline(player);
    }

    /** Force close+open even when type/mode match (SignGUI handoff prep, etc.). */
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
        scheduler.runForPlayer(player, new CloseThenStartTask(this, player, delay, onSettled));
    }

    public java.util.concurrent.CompletableFuture<Void> closeAsync(Player player) {
        return closeAsync(player, 1L);
    }

    public java.util.concurrent.CompletableFuture<Void> closeAsync(Player player, long settleTicks) {
        java.util.concurrent.CompletableFuture<Void> future = new java.util.concurrent.CompletableFuture<>();
        closeThen(player, settleTicks, new CompleteFuture(future));
        return future;
    }

    public void updateTitle(Player player, Component title) {
        scheduler.runForPlayer(player, new UpdateTitleTask(this, player, title));
    }

    public void onCloseMenu(Player player) {
        onCloseMenu(player, GuiCloseReason.PLAYER);
    }

    public void onCloseMenu(Player player, GuiCloseReason reason) {
        // Never soft-ignore: a stranded transition/session blocks opens and Bukkit clicks.
        closeCurrent(
                player,
                false,
                true,
                reason == null ? GuiCloseReason.PLAYER : reason,
                true,
                true
        );
    }

    public void closeMenu(Player player) {
        scheduler.runForPlayer(player, new CloseMenuTask(this, player));
    }

    /**
     * Open a written-book text GUI. Closes any packet inventory menu first.
     * See {@link BookView} for vanilla limits and close-hook semantics.
     */
    public void openBook(Player player, BookView view) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(view, "view");
        scheduler.runForPlayer(player, new OpenBookTask(this, player, view));
    }

    public boolean hasBookOpen(UUID playerId) {
        return bookViewers.contains(playerId);
    }

    public boolean hasBookOpen(Player player) {
        return player != null && hasBookOpen(player.getUniqueId());
    }

    void openBookSync(Player player, BookView view) {
        closeCurrent(player, true, true, GuiCloseReason.REPLACE, true);
        endBookView(player, true);
        boolean opened = BookOpener.open(player, view, adapter.packets());
        if (!opened) {
            debug(player, "BOOK_OPEN failed (no Audience.openBook / NMS / ItemStack fallback)");
            return;
        }
        UUID pid = id(player);
        bookViewers.add(pid);
        if (view.onClose() != null) {
            bookOnClose.put(pid, view.onClose());
        } else {
            bookOnClose.remove(pid);
        }
        if (debugLogging) {
            debug(player, "BOOK_OPEN pages=" + view.pages().size());
        }
    }

    private void endBookView(Player player, boolean fireClose) {
        UUID pid = id(player);
        if (!bookViewers.remove(pid)) {
            bookOnClose.remove(pid);
            return;
        }
        java.util.function.Consumer<Player> hook = bookOnClose.remove(pid);
        if (fireClose && hook != null) {
            try {
                hook.accept(player);
            } catch (Throwable ignored) {
            }
        }
    }

    private void closeCurrent(Player player, boolean sendClosePacket, boolean reclaim) {
        closeCurrent(player, sendClosePacket, reclaim, GuiCloseReason.API, true, true);
    }

    private void closeCurrent(Player player, boolean sendClosePacket, boolean reclaim, GuiCloseReason reason) {
        closeCurrent(player, sendClosePacket, reclaim, reason, true, true);
    }

    private void closeCurrent(
            Player player,
            boolean sendClosePacket,
            boolean reclaim,
            GuiCloseReason reason,
            boolean releaseWindowId
    ) {
        closeCurrent(player, sendClosePacket, reclaim, reason, releaseWindowId, true);
    }

    private void closeCurrent(
            Player player,
            boolean sendClosePacket,
            boolean reclaim,
            GuiCloseReason reason,
            boolean releaseWindowId,
            boolean fireMenuOnClose
    ) {
        UUID pid = id(player);
        if (!closingPlayers.add(pid)) {
            // Nested close (onClose → close/present): outer owns cleanup; drop leftovers.
            MenuSession nested = sessions.remove(pid);
            if (nested != null) {
                try {
                    adapter.packets().unbindServerContainer(player);
                } catch (Throwable ignored) {
                }
            }
            return;
        }
        MenuSession session = sessions.get(pid);
        int top = 0;
        int windowId = -1;
        CloseSnapshot snapshot = null;
        try {
            if (session == null) {
                return;
            }
            session.setPhase(SessionPhase.CLOSING);
            // Include bottomHeld so CloseSnapshot.cursor matches what the player sees.
            UxItem cursor = activeCursor(player);
            snapshot = new CloseSnapshot(session.menu().items(), cursor);
            BiConsumer<Player, CloseSnapshot> onClose = session.menu().onClose();
            if (fireMenuOnClose && onClose != null) {
                try {
                    onClose.accept(player, snapshot);
                } catch (Throwable ignored) {
                }
            }
            if (reclaim && reclaimCursorOnClose && cursor != null && !cursor.isEmpty()) {
                // Single reclaim — covers carriedItem and bottomHeld (activeCursor).
                CursorReclaim.reclaim(player, adapter.items(), cursor);
            }
            // Empty top on the client before CloseWindow so vanilla does not dump GUI items
            // back into the player inventory (would duplicate plugin onClose refunds).
            if (sendClosePacket) {
                try {
                    flushClientTopEmpty(player, session);
                } catch (Throwable error) {
                    debug(player, "flushClientTopEmpty failed: " + error.getClass().getSimpleName());
                }
            }
            try {
                adapter.packets().sendCursorItem(player, UxItem.EMPTY);
            } catch (Throwable ignored) {
            }
            if (sendClosePacket) {
                try {
                    adapter.packets().sendCloseWindow(player, session.windowId());
                } catch (Throwable error) {
                    debug(player, "sendCloseWindow failed: " + error.getClass().getSimpleName());
                }
            }
            try {
                adapter.packets().unbindServerContainer(player);
            } catch (Throwable ignored) {
            }
            top = session.topSlotCount();
            windowId = session.windowId();
            if (debugLogging) {
                debug(player, "CLOSE windowId=" + windowId + " sendPacket=" + sendClosePacket
                        + " reclaim=" + reclaim + " releaseId=" + releaseWindowId
                        + " fireOnClose=" + fireMenuOnClose + " reason=" + reason);
            }
            if (snapshot != null) {
                fireScope(player, false, session, top, reason, snapshot);
            }
        } catch (Throwable error) {
            debug(player, "closeCurrent failed: " + error.getClass().getSimpleName());
        } finally {
            // Always clear tracking — packet/send failures must not strand CLOSING/OPEN.
            clearPlayerTracking(player, releaseWindowId);
            closingPlayers.remove(pid);
            Menu pending = pendingPresent.remove(pid);
            if (pending != null && player.isOnline()) {
                try {
                    installOpen(player, pending);
                } catch (Throwable error) {
                    debug(player, "pendingPresent failed: " + error.getClass().getSimpleName());
                }
            }
        }
    }

    private void clearPlayerTracking(Player player, boolean releaseWindowId) {
        UUID pid = id(player);
        sessions.remove(pid);
        if (releaseWindowId) {
            windowIds.release(player);
        }
        carriedItem.remove(pid);
        clearBottomHeld(player);
        clearAccumulatedDrag(player);
        nettyRoCorrectedForState.remove(pid);
        bottomCache.remove(pid);
        lastClickDecision.remove(pid);
        ignoreInboundCloseUntilNanos.remove(pid);
    }

    /** Clear top slots client-side so CloseWindow cannot dump virtual items into the inv. */
    private void flushClientTopEmpty(Player player, MenuSession session) {
        Menu menu = session.menu();
        int top = menu.type().protocolTopSize();
        if (top <= 0) {
            return;
        }
        int windowId = session.windowId();
        int stateId = protocolState(player, session);
        List<UxItem> bottom = bottomCache.get(id(player));
        if (bottom == null || bottom.size() != menu.type().bottomSlotCount()) {
            bottom = snapshotBottom(player);
            bottomCache.put(id(player), bottom);
        }
        List<UxItem> emptied = new ArrayList<>(top + bottom.size());
        for (int i = 0; i < top; i++) {
            emptied.add(UxItem.EMPTY);
        }
        emptied.addAll(bottom);
        adapter.packets().mirrorTopSlots(player, emptied.subList(0, top));
        if (!adapter.packets().sendBoundAuthority(player, stateId, true)) {
            adapter.packets().sendWindowItems(player, windowId, stateId, emptied, UxItem.EMPTY);
        }
    }

    private void fireOpenEvent(Player player, MenuSession session, GuiOpenReason reason) {
        if (session == null || !events.hasOpenListeners()) {
            return;
        }
        try {
            events.fireOpen(new GuiOpenEvent(
                    player,
                    session.menu(),
                    session.windowId(),
                    session.topSlotCount(),
                    session.stateId(),
                    reason
            ));
        } catch (Throwable ignored) {
        }
    }

    private void fireScope(
            Player player,
            boolean open,
            MenuSession session,
            int topSlotCount,
            GuiCloseReason closeReason,
            CloseSnapshot snapshot
    ) {
        GuiScopeListener listener = scopeListener;
        if (listener != null) {
            try {
                listener.onScopeChanged(player, open, topSlotCount);
            } catch (Throwable ignored) {
            }
        }
        if (session == null) {
            return;
        }
        try {
            if (open) {
                fireOpenEvent(player, session, GuiOpenReason.OPEN);
            } else {
                if (!events.hasCloseListeners()) {
                    return;
                }
                events.fireClose(new GuiCloseEvent(
                        player,
                        session.menu(),
                        session.windowId(),
                        topSlotCount,
                        session.stateId(),
                        closeReason,
                        snapshot
                ));
            }
        } catch (Throwable ignored) {
        }
    }

    private void applyMenuDifferential(Player player, MenuSession session, Menu next) {
        Menu current = session.menu();
        boolean titleChanged = !current.name().equals(next.name());
        if (titleChanged) {
            current.setName(next.name());
            session.setTitle(next.name());
            adapter.packets().sendOpenWindow(player, session.windowId(), next.type().id(), next.name());
        }
        current.buttons().clear();
        current.buttons().putAll(next.buttons());
        List<UxItem> before = current.items();
        List<UxItem> after = next.items();
        current.setItems(after);
        adapter.items().preload(after);
        int size = next.type().size();
        int dirtyCount = 0;
        // Prefer SetSlot for sparse updates; avoid HashMap alloc when possible.
        int[] dirtySlots = null;
        UxItem[] dirtyItems = null;
        for (int i = 0; i < size; i++) {
            UxItem a = i < before.size() ? before.get(i) : UxItem.EMPTY;
            UxItem b = i < after.size() ? after.get(i) : UxItem.EMPTY;
            if (a == b || (a != null && a.equals(b))) {
                continue;
            }
            if (dirtySlots == null) {
                dirtySlots = new int[Math.min(16, size)];
                dirtyItems = new UxItem[dirtySlots.length];
            } else if (dirtyCount == dirtySlots.length) {
                int grow = Math.min(size, dirtySlots.length * 2);
                dirtySlots = java.util.Arrays.copyOf(dirtySlots, grow);
                dirtyItems = java.util.Arrays.copyOf(dirtyItems, grow);
            }
            dirtySlots[dirtyCount] = i;
            dirtyItems[dirtyCount] = b == null ? UxItem.EMPTY : b;
            dirtyCount++;
        }
        if (dirtyCount == 0 && !titleChanged) {
            return;
        }
        if (titleChanged || dirtyCount > 24) {
            // OpenWindow requires a full content follow-up; dense redraws too.
            int stateId = protocolState(player, session);
            adapter.packets().mirrorTopSlots(player, after);
            if (!adapter.packets().sendBoundAuthority(player, stateId,
                    session.menu().mode() != MenuMode.EDITABLE)) {
                adapter.packets().sendWindowItems(
                        player,
                        session.windowId(),
                        stateId,
                        contentsForOpen(player, current),
                        carriedItem.get(id(player))
                );
            }
        } else {
            int stateId = protocolState(player, session);
            int windowId = session.windowId();
            for (int i = 0; i < dirtyCount; i++) {
                adapter.packets().sendSetSlot(player, windowId, stateId, dirtySlots[i], dirtyItems[i]);
            }
        }
        session.bumpGeneration();
    }

    public void handleIncomingClick(Player player, ClickPacket packet) {
        UUID pid = id(player);
        if (closingPlayers.contains(pid)) {
            return;
        }
        MenuSession session = sessions.get(pid);
        if (session == null) {
            return;
        }
        if (packet.windowId() != session.windowId()) {
            emitDecision(player, packet, SlotKind.DECORATIVE, false, false, "stale_window_id");
            return;
        }
        if (!isValidClickSlot(session, packet)) {
            emitDecision(player, packet, SlotKind.DECORATIVE, false, false, "invalid_slot_resync_full");
            resyncFull(player, session, packet.stateId(), true);
            return;
        }
        if (session.phase() != SessionPhase.OPEN) {
            emitDecision(player, packet, SlotKind.DECORATIVE, false, false, "phase_mismatch_drop");
            return;
        }
        final int clickGeneration = session.generation();
        long now = System.nanoTime();
        // EDITABLE take→place is faster than debounce — never reject simulation.
        boolean debounced = clickDebounceNanos > 0L
                && session.lastClickNanos() > 0L
                && now - session.lastClickNanos() < clickDebounceNanos;
        if (debounced && session.menu().mode() != MenuMode.EDITABLE) {
            emitDecision(player, packet, SlotKind.DECORATIVE, false, false, "debounce_resync_full");
            if (stillSameSession(player, session, clickGeneration)) {
                resyncFull(player, session, packet.stateId(), true);
            }
            return;
        }
        session.markClick(now);

        ClickData clickData = getClickType(packet);
        UxItem carried = activeCursor(player);
        boolean listenClick = events.hasClickListeners();
        if (listenClick) {
            GuiClickEvent pre = new GuiClickEvent(
                    player,
                    session.menu(),
                    session.windowId(),
                    session.topSlotCount(),
                    session.stateId(),
                    packet,
                    clickData,
                    carried
            );
            events.fireClick(pre);
            if (pre.isCancelled()) {
                emitDecision(player, packet, SlotKind.DECORATIVE, false, false, "listener_cancelled");
                if (stillSameSession(player, session, clickGeneration)) {
                    resyncFull(player, session, packet.stateId(), session.menu().mode() != MenuMode.EDITABLE);
                    fireClickPost(player, session, packet, clickData, carried, "listener_cancelled");
                }
                return;
            }
        }

        if (clickData.clickType() == ClickType.DRAG_START
                || clickData.clickType() == ClickType.DRAG_ADD
                || clickData.clickType() == ClickType.DRAG_END) {
            GuiDragPhase phase = switch (clickData.clickType()) {
                case DRAG_START -> GuiDragPhase.START;
                case DRAG_END -> GuiDragPhase.END;
                default -> GuiDragPhase.ADD;
            };
            if (events.hasDragListeners()) {
                Set<Integer> dragSlots = new HashSet<>(4);
                if (packet.slot() >= 0) {
                    dragSlots.add(packet.slot());
                }
                dragSlots.addAll(packet.changedSlotIds());
                GuiDragEvent drag = new GuiDragEvent(
                        player,
                        session.menu(),
                        session.windowId(),
                        session.topSlotCount(),
                        session.stateId(),
                        packet,
                        clickData,
                        phase,
                        dragSlots,
                        carried
                );
                events.fireDrag(drag);
                if (drag.isCancelled()) {
                    clearAccumulatedDrag(player);
                    if (stillSameSession(player, session, clickGeneration)) {
                        resyncFull(player, session, packet.stateId(), session.menu().mode() != MenuMode.EDITABLE);
                        fireClickPost(player, session, packet, clickData, carried, "drag_cancelled");
                    }
                    return;
                }
            }
            if (phase == GuiDragPhase.START || phase == GuiDragPhase.ADD) {
                if (!stillSameSession(player, session, clickGeneration)) {
                    return;
                }
                accumulateDrag(player, packet, clickData.clickType());
                if (session.menu().mode() != MenuMode.EDITABLE) {
                    resyncDirtySlots(player, session, packet, UxItem.EMPTY);
                    carriedItem.remove(id(player));
                }
                fireClickPost(player, session, packet, clickData, carried, "drag_accumulate");
                return;
            }
            // DRAG_END falls through to editable/readonly handlers below
        }
        if (!stillOpenSession(player, session)) {
            emitDecision(player, packet, SlotKind.DECORATIVE, false, false, "generation_stale_pre");
            return;
        }
        if (session.menu().mode() == MenuMode.EDITABLE) {
            handleEditableClick(player, session, clickData, packet);
            if (stillOpenSession(player, session)) {
                fireClickPost(player, session, packet, clickData, activeCursor(player),
                        lastClickDecision.getOrDefault(id(player), "editable"));
            }
            return;
        }
        if (isMenuClick(packet, clickData.clickType(), player)) {
            handleClickMenu(player, session, clickData, packet);
            if (stillOpenSession(player, session)) {
                fireClickPost(player, session, packet, clickData, UxItem.EMPTY,
                        lastClickDecision.getOrDefault(id(player), "readonly"));
            }
            return;
        }
        settleReadOnlyOutside(player, session, packet);
        if (stillOpenSession(player, session)) {
            fireClickPost(player, session, packet, clickData, UxItem.EMPTY, "readonly_outside");
        }
    }

    private boolean stillSameSession(Player player, MenuSession expected, int generation) {
        if (expected == null || closingPlayers.contains(id(player))) {
            return false;
        }
        MenuSession current = sessions.get(id(player));
        return current == expected
                && current.phase() == SessionPhase.OPEN
                && current.generation() == generation
                && current.windowId() == expected.windowId();
    }

    /** Session still open (generation may bump from resync within the same click). */
    private boolean stillOpenSession(Player player, MenuSession expected) {
        if (expected == null || closingPlayers.contains(id(player))) {
            return false;
        }
        MenuSession current = sessions.get(id(player));
        return current == expected
                && current.phase() == SessionPhase.OPEN
                && current.windowId() == expected.windowId();
    }

    private void fireClickPost(
            Player player,
            MenuSession session,
            ClickPacket packet,
            ClickData clickData,
            UxItem carried,
            String decision
    ) {
        if (!events.hasClickPostListeners()) {
            return;
        }
        try {
            events.fireClickPost(new GuiClickPostEvent(
                    player,
                    session.menu(),
                    session.windowId(),
                    session.topSlotCount(),
                    session.stateId(),
                    packet,
                    clickData,
                    carried,
                    decision
            ));
        } catch (Throwable ignored) {
        }
    }

    private boolean isValidClickSlot(MenuSession session, ClickPacket packet) {
        int slot = packet.slot();
        if (slot == -999 || slot == -1) {
            return true;
        }
        int max = session.menu().type().totalProtocolSlots();
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
        int maxSlot = topSize + menu.type().bottomSlotCount();
        List<UxItem> bottom = snapshotBottom(player);
        int windowId = session.windowId();
        int stateId = protocolState(player, session, packet.stateId());
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

        SlotKind kind = session.slotKind(packet.slot());
        if (kind == SlotKind.ACTION || kind == SlotKind.DECORATIVE) {
            // Buttons must not keep a floating inv cursor — return it first.
            if (bottomHeld.containsKey(id(player))) {
                restoreBottomHeld(player, session);
            }
            UxItem at = packet.slot() < menu.items().size() ? menu.items().get(packet.slot()) : UxItem.EMPTY;
            UxItem cursor = carriedItem.getOrDefault(id(player), UxItem.EMPTY);
            boolean taking = cursor.isEmpty() && !at.isEmpty() && isTakeableItem(at);
            if (taking && type == WindowClickType.PICKUP) {
                Predicate<Integer> allow = new SingleSlotAllow(packet.slot());
                VirtualClickSimulator.Result result = VirtualClickSimulator.simulate(
                        menu.items(), cursor, packet.slot(), packet.button(), type, allow
                );
                emitDecision(player, packet, kind, false, true, "takeable_pickup_simulate");
                applyEditableResult(player, session, result, packet.stateId());
                return;
            }
            if (taking && type == WindowClickType.QUICK_MOVE) {
                handleShiftFromTopToBottom(player, session, packet);
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

        if (kind == SlotKind.EXTRACTABLE) {
            handleExtractableTopClick(player, session, packet, type);
            return;
        }

        // EDITABLE — both directions. Cursor may be carriedItem OR bottomHeld (inv pickup).
        if (type == WindowClickType.QUICK_MOVE) {
            if (bottomHeld.containsKey(id(player))) {
                restoreBottomHeld(player, session);
            }
            handleShiftFromTopToBottom(player, session, packet);
            return;
        }

        UUID pid = id(player);
        EditableBottomMoves.Held held = bottomHeld.get(pid);
        boolean fromBottomHeld = held != null && !held.isEmpty();
        UxItem cursor = fromBottomHeld ? held.item() : carriedItem.getOrDefault(pid, UxItem.EMPTY);
        Predicate<Integer> allow = cursor.isEmpty()
                ? new SessionAllowTake(session)
                : new SessionAllowPlace(session);
        if (!cursor.isEmpty() && !session.allowsPlace(packet.slot())) {
            rejectEditable(player, session, packet);
            return;
        }
        VirtualClickSimulator.Result result = VirtualClickSimulator.simulate(
                menu.items(),
                cursor,
                packet.slot(),
                packet.button(),
                type,
                allow
        );
        if (fromBottomHeld) {
            // Item left the player-inv hold and is now on the top cursor / placed in GUI.
            bottomHeld.remove(pid);
        }
        emitDecision(player, packet, kind, false, false,
                fromBottomHeld ? "editable_place_from_inv_cursor" : "editable_simulate");
        applyEditableResult(player, session, result, packet.stateId());
    }

    private void handleExtractableTopClick(
            Player player,
            MenuSession session,
            ClickPacket packet,
            WindowClickType type
    ) {
        UxItem cursor = activeCursor(player);
        if (!cursor.isEmpty()) {
            // Cannot place into extractable slots.
            emitDecision(player, packet, SlotKind.EXTRACTABLE, false, false, "extractable_reject_place");
            rejectEditable(player, session, packet);
            return;
        }
        if (type == WindowClickType.QUICK_MOVE) {
            handleShiftFromTopToBottom(player, session, packet);
            return;
        }
        if (type != WindowClickType.PICKUP) {
            rejectEditable(player, session, packet);
            return;
        }
        VirtualClickSimulator.Result result = VirtualClickSimulator.simulate(
                session.menu().items(),
                cursor,
                packet.slot(),
                packet.button(),
                type,
                new SessionAllowTake(session)
        );
        emitDecision(player, packet, SlotKind.EXTRACTABLE, false, false, "extractable_take");
        applyEditableResult(player, session, result, packet.stateId());
    }

    /** Shift-click top → player inventory (gui→inv). */
    private void handleShiftFromTopToBottom(Player player, MenuSession session, ClickPacket packet) {
        if (!session.allowsTake(packet.slot())) {
            rejectEditable(player, session, packet);
            return;
        }
        Menu menu = session.menu();
        List<UxItem> top = new java.util.ArrayList<>(menu.items());
        while (top.size() < menu.type().protocolTopSize()) {
            top.add(UxItem.EMPTY);
        }
        int slot = packet.slot();
        UxItem moving = slot < top.size() ? top.get(slot) : UxItem.EMPTY;
        if (moving == null || moving.isEmpty()) {
            rejectEditable(player, session, packet);
            return;
        }
        top.set(slot, UxItem.EMPTY);
        List<UxItem> bottom = snapshotBottom(player);
        VirtualClickSimulator.Result inserted = VirtualClickSimulator.shiftInto(
                bottom, moving, Predicates.ALWAYS_TRUE_INT);
        menu.setItems(top);
        syncButtonsFromItems(menu, top, Set.of(slot));
        writeBottom(player, inserted.items());
        int windowId = session.windowId();
        int stateId = protocolState(player, session, packet.stateId());
        int topSize = menu.type().protocolTopSize();
        adapter.packets().sendSetSlot(player, windowId, stateId, slot, UxItem.EMPTY);
        for (Integer dirty : inserted.dirty()) {
            if (dirty == null || dirty < 0 || dirty >= inserted.items().size()) {
                continue;
            }
            adapter.packets().sendSetSlot(
                    player, windowId, stateId, topSize + dirty, inserted.items().get(dirty));
        }
        UxItem leftover = inserted.cursor();
        if (leftover != null && !leftover.isEmpty()) {
            // No room in inv — put remainder back on top slot.
            top.set(slot, leftover);
            menu.setItems(top);
            syncButtonsFromItems(menu, top, Set.of(slot));
            adapter.packets().sendSetSlot(player, windowId, stateId, slot, leftover);
        }
        carriedItem.remove(id(player));
        adapter.packets().sendCursorItem(player, UxItem.EMPTY);
        emitDecision(player, packet, session.slotKind(slot), false, false, "shift_top_to_bottom");
    }

    private void handleShiftFromBottom(Player player, MenuSession session, ClickPacket packet) {
        if (bottomHeld.containsKey(id(player))) {
            restoreBottomHeld(player, session);
        }
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
                new SessionAllowPlace(session)
        );
        UxItem leftover = inserted.cursor();
        menu.setItems(inserted.items());
        syncButtonsFromItems(menu, inserted.items(), inserted.dirty());
        bottom.set(bottomIndex, leftover);
        writeBottom(player, bottom);
        int windowId = session.windowId();
        int stateId = protocolState(player, session, packet.stateId());
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
        int maxSlot = topSize + session.menu().type().bottomSlotCount();
        int windowId = session.windowId();
        int stateId = protocolState(player, session, packet.stateId());
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
        UxItem topCursor = carriedItem.getOrDefault(pid, UxItem.EMPTY);
        int topSize = session.menu().type().protocolTopSize();
        int maxSlot = topSize + session.menu().type().bottomSlotCount();
        int slot = packet.slot();
        if (slot < topSize || slot >= maxSlot) {
            settleEditableBottom(player, session, packet);
            return;
        }
        int bottomIndex = slot - topSize;
        List<UxItem> bottom = snapshotBottom(player);

        // Place virtual top-cursor into player inv (gui→inv click).
        if (!topCursor.isEmpty()) {
            VirtualClickSimulator.Result placed = VirtualClickSimulator.simulate(
                    bottom,
                    topCursor,
                    bottomIndex,
                    packet.button(),
                    WindowClickType.PICKUP,
                    Predicates.ALWAYS_TRUE_INT
            );
            writeBottom(player, placed.items());
            if (placed.cursor().isEmpty()) {
                carriedItem.remove(pid);
            } else {
                carriedItem.put(pid, placed.cursor());
            }
            int windowId = session.windowId();
            int stateId = protocolState(player, session, packet.stateId());
            for (Integer dirty : placed.dirty()) {
                if (dirty == null || dirty < 0 || dirty >= placed.items().size()) {
                    continue;
                }
                adapter.packets().sendSetSlot(
                        player, windowId, stateId, topSize + dirty, placed.items().get(dirty));
            }
            adapter.packets().sendCursorItem(player, placed.cursor());
            return;
        }

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
        int stateId = protocolState(player, session, packet.stateId());
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
        int stateId = protocolState(player, session);
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
        int storage = LiveLimits.playerStorageSlots();
        int hotbar = LiveLimits.hotbarSlots();
        int total = LiveLimits.playerInventorySlots();
        for (int i = 0; i < storage && i < bottom.size(); i++) {
            inv.setItem(i + hotbar, toBukkitOrNull(bottom.get(i)));
        }
        for (int i = 0; i < hotbar && (storage + i) < bottom.size(); i++) {
            inv.setItem(i, toBukkitOrNull(bottom.get(storage + i)));
        }
        // Cache what we wrote — do not re-snapshot Bukkit.
        if (bottom.size() == total) {
            bottomCache.put(id(player), bottom.getClass() == ArrayList.class
                    ? List.copyOf(bottom)
                    : bottom);
        } else {
            refreshBottomCache(player);
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
            if (slot < 0 || slot > menu.type().protocolLastIndex() || !session.allowsPlace(slot)) {
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
            if (only == null || !session.allowsPlace(only)) {
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
                new SessionAllowPlace(session)
        );
        applyEditableResult(player, session, result, packet.stateId());
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
            execute.accept(new ExecuteComponent(
                    player,
                    clickData.buttonType(),
                    clickData.clickType(),
                    packet.slot(),
                    cursor,
                    cursor,
                    session.topSlotCount()
            ));
        }
    }

    private void applyEditableResult(
            Player player,
            MenuSession session,
            VirtualClickSimulator.Result result,
            int clientStateId
    ) {
        UUID pid = id(player);
        Menu menu = session.menu();
        boolean dirtyEmpty = result.dirty().isEmpty();
        UxItem nextCursor = result.cursor();
        UxItem prevCursor = carriedItem.getOrDefault(pid, UxItem.EMPTY);
        if (!dirtyEmpty) {
            menu.setItems(result.items());
            syncButtonsFromItems(menu, result.items(), result.dirty());
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
        int stateId = protocolState(player, session, clientStateId);
        if (!dirtyEmpty) {
            List<UxItem> items = result.items();
            for (Integer slot : result.dirty()) {
                UxItem item = slot < items.size() ? items.get(slot) : UxItem.EMPTY;
                adapter.packets().sendSetSlot(player, windowId, stateId, slot, item);
            }
        }
        adapter.packets().sendCursorItem(player, nextCursor);
    }

    private void syncButtonsFromItems(Menu menu, List<UxItem> items, Set<Integer> dirty) {
        if (dirty == null || dirty.isEmpty()) {
            return;
        }
        Map<Integer, Button> buttons = menu.buttons();
        for (Integer slot : dirty) {
            if (slot == null || slot < 0) {
                continue;
            }
            UxItem item = slot < items.size() ? items.get(slot) : UxItem.EMPTY;
            if (item == null) {
                item = UxItem.EMPTY;
            }
            Button existing = buttons.get(slot);
            if (existing != null) {
                buttons.put(slot, new Button(item, existing.execute(), existing.cooldown(), existing.kind()));
            } else if (menu.mode() == MenuMode.EDITABLE) {
                buttons.put(slot, new Button(
                        item,
                        null,
                        new CooldownComponent(),
                        SlotKind.EDITABLE
                ));
            }
        }
    }

    private void rejectEditable(Player player, MenuSession session, ClickPacket packet) {
        Menu menu = session.menu();
        int last = menu.type().protocolLastIndex();
        int topSize = menu.type().protocolTopSize();
        int maxSlot = topSize + menu.type().bottomSlotCount();
        int windowId = session.windowId();
        int stateId = protocolState(player, session, packet.stateId());
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
        int stateId = clientStateId >= 0
                ? protocolState(player, session, clientStateId)
                : protocolState(player, session);
        if (clearCursor) {
            carriedItem.remove(id(player));
            clearBottomHeld(player);
        }
        if (adapter.packets().sendBoundAuthority(player, stateId, clearCursor)) {
            return;
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
     * Immediate server→client authority (Netty-safe). Prefers direct NMS bound-slot sync.
     */
    public void suppressClientPrediction(Player player, ClickPacket packet) {
        MenuSession session = sessions.get(id(player));
        if (session == null || session.phase() != SessionPhase.OPEN) {
            return;
        }
        int clientState = packet == null ? 0 : Math.max(0, packet.stateId());
        int provisional = Math.max(session.stateId(), clientState) + 1;
        boolean clearCursor = session.menu().mode() != MenuMode.EDITABLE;
        if (!adapter.packets().sendBoundAuthority(player, provisional, clearCursor)) {
            adapter.packets().sendWindowItems(
                    player,
                    session.windowId(),
                    provisional,
                    nettySafeContents(player, session),
                    null
            );
            if (clearCursor) {
                adapter.packets().sendCursorItem(player, UxItem.EMPTY);
            }
        }
        if (clearCursor) {
            carriedItem.remove(id(player));
        }
        if (packet != null) {
            nettyRoCorrectedForState.put(id(player), packet.stateId());
        }
    }

    /** @deprecated use {@link #suppressClientPrediction(Player, ClickPacket)} */
    @Deprecated
    public void correctReadOnlyClick(Player player, ClickPacket packet) {
        suppressClientPrediction(player, packet);
    }

    /** Full resync for open session (window-id mismatch / safety). */
    public void forceResyncOpen(Player player) {
        MenuSession session = sessions.get(id(player));
        if (session == null || session.phase() != SessionPhase.OPEN) {
            return;
        }
        boolean clearCursor = session.menu().mode() != MenuMode.EDITABLE;
        resyncFull(player, session, session.stateId(), clearCursor);
        if (debugLogging) {
            debug(player, "forceResyncOpen mode=" + session.menu().mode() + " windowId=" + session.windowId());
        }
    }

    private void refreshBottomCache(Player player) {
        bottomCache.put(id(player), snapshotBottom(player));
    }

    private List<UxItem> nettySafeContents(Player player, MenuSession session) {
        List<UxItem> bottom = bottomCache.get(id(player));
        int expected = LiveLimits.playerInventorySlots();
        if (bottom == null || bottom.size() != expected) {
            bottom = emptyBottom();
        }
        return assembleContents(session.menu(), bottom);
    }

    private static List<UxItem> emptyBottom() {
        int n = LiveLimits.playerInventorySlots();
        UxItem[] arr = new UxItem[n];
        java.util.Arrays.fill(arr, UxItem.EMPTY);
        return java.util.Arrays.asList(arr);
    }

    private List<UxItem> assembleContents(Menu menu, List<UxItem> bottom) {
        int top = menu.type().protocolTopSize();
        int bottomCount = menu.type().bottomSlotCount();
        List<UxItem> contents = new ArrayList<>(top + bottomCount);
        List<UxItem> items = menu.items();
        for (int i = 0; i < top; i++) {
            UxItem item = i < items.size() ? items.get(i) : null;
            contents.add(item == null ? UxItem.EMPTY : item);
        }
        for (int i = 0; i < bottomCount; i++) {
            if (bottom != null && i < bottom.size()) {
                UxItem item = bottom.get(i);
                contents.add(item == null ? UxItem.EMPTY : item);
            } else {
                contents.add(UxItem.EMPTY);
            }
        }
        return contents;
    }

    private void ensurePipeline(Player player) {
        Consumer<Player> reassert = pipelineReassert;
        if (reassert == null) {
            return;
        }
        try {
            reassert.accept(player);
        } catch (Throwable error) {
            debug(player, "pipeline ensure failed: " + error.getClass().getSimpleName());
        }
    }

    private List<UxItem> contentsForOpen(Player player, Menu menu) {
        return fullContents(player, menu);
    }

    private List<UxItem> fullContents(Player player, Menu menu) {
        List<UxItem> bottom = bottomCache.get(id(player));
        int expected = LiveLimits.playerInventorySlots();
        if (bottom == null || bottom.size() != expected) {
            bottom = snapshotBottom(player);
            bottomCache.put(id(player), bottom);
        }
        return assembleContents(menu, bottom);
    }

    private List<UxItem> snapshotBottom(Player player) {
        int hotbar = LiveLimits.hotbarSlots();
        int total = LiveLimits.playerInventorySlots();
        List<UxItem> bottom = new ArrayList<>(total);
        PlayerInventory inv = player.getInventory();
        for (int i = hotbar; i < total; i++) {
            bottom.add(fromBukkitSlot(inv.getItem(i)));
        }
        for (int i = 0; i < hotbar; i++) {
            bottom.add(fromBukkitSlot(inv.getItem(i)));
        }
        // Ensure size matches NMS inventory size even if loops diverge.
        while (bottom.size() < total) {
            bottom.add(UxItem.EMPTY);
        }
        if (bottom.size() > total) {
            return List.copyOf(bottom.subList(0, total));
        }
        return bottom;
    }

    private UxItem fromBukkitSlot(ItemStack stack) {
        if (stack == null || stack.getAmount() <= 0 || stack.getType() == org.bukkit.Material.AIR) {
            return UxItem.EMPTY;
        }
        return adapter.items().fromBukkit(stack);
    }

    public void handleClickMenu(Player player, ClickData clickData, int slot) {
        MenuSession session = requireSession(player);
        handleClickMenu(player, session, clickData, syntheticClick(session, slot));
    }

    private void handleClickMenu(Player player, MenuSession session, ClickData clickData, ClickPacket packet) {
        if (clickData.clickType() == ClickType.DRAG_END) {
            clearAccumulatedDrag(player);
        }
        if (!stillOpenSession(player, session)) {
            emitDecision(player, packet, SlotKind.DECORATIVE, false, false, "readonly_stale_session");
            return;
        }
        Menu menu = session.menu();
        int slot = packet.slot();
        Button button = slot >= 0 ? menu.buttons().get(slot) : null;
        // Netty already sent bound authority for this client stateId — skip duplicate SetContent.
        Integer corrected = nettyRoCorrectedForState.get(id(player));
        if (corrected == null || corrected != packet.stateId()) {
            resyncFull(player, session, packet.stateId(), true);
        } else {
            protocolState(player, session, packet.stateId());
            carriedItem.remove(id(player));
        }
        // resyncFull bumps generation — only require same open session identity.
        if (!stillOpenSession(player, session)) {
            emitDecision(player, packet, SlotKind.DECORATIVE, false, false, "readonly_closed_during_resync");
            return;
        }
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
            UxItem at = slot >= 0 && slot < session.menu().items().size()
                    ? session.menu().items().get(slot)
                    : UxItem.EMPTY;
            execute.accept(new ExecuteComponent(
                    player,
                    clickData.buttonType(),
                    clickData.clickType(),
                    slot,
                    at,
                    UxItem.EMPTY,
                    session.topSlotCount()
            ));
        } else {
            emitDecision(player, packet, SlotKind.ACTION, true, false, "readonly_cooldown_block");
        }
    }

    public void refreshWindow(Player player) {
        scheduler.runForPlayer(player, new RefreshWindowTask(this, player));
    }

    public void updateItem(Player player, UxItem item, int slot) {
        scheduler.runForPlayer(player, new UpdateItemTask(this, player, item, slot));
    }

    public void updateItems(Player player, Map<Integer, UxItem> newItems) {
        scheduler.runForPlayer(player, new UpdateItemsTask(this, player, newItems));
    }

    public void updateButton(Player player, Button newButton, int slot) {
        scheduler.runForPlayer(player, new UpdateButtonTask(this, player, newButton, slot));
    }

    public void updateButtons(Player player, Map<Integer, Button> newButtons) {
        scheduler.runForPlayer(player, new UpdateButtonsTask(this, player, newButtons));
    }

    public void updateButtonsBySlot(Player player, Map<Integer, Button> patches) {
        scheduler.runForPlayer(player, new UpdateButtonsBySlotTask(this, player, patches));
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
        scheduler.runForPlayer(player, new PatchSlotAtomicTask(this, player, slot, item, buttonBuilder, slotKind));
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
                // Post-click carried: empty → place finished; non-empty → pickup (match left).
                yield packet.carriedEmpty() ? ClickData.RIGHT_PLACE : ClickData.RIGHT_PICKUP;
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
        accumulatedDrag.computeIfAbsent(id(player), NEW_DRAG_LIST).add(new AccumulatedDrag(packet, type));
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
        lastClickDecision.put(id(player), result);
        if (debugLogging) {
            debug(player, "click slot=" + packet.slot() + " kind=" + slotKind
                    + " handler=" + handlerFound + " takeable=" + takeable + " -> " + result);
        }
    }

    private void resyncDirtySlots(Player player, MenuSession session, ClickPacket packet, UxItem carried) {
        Menu menu = session.menu();
        int last = menu.type().protocolLastIndex();
        int windowId = session.windowId();
        int stateId = protocolState(player, session, packet.stateId());
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

    /**
     * Prefer NMS {@code incrementStateId} when a ChestMenu is bound; otherwise session counter.
     */
    private int protocolState(Player player, MenuSession session, int clientFloor) {
        int bridged = adapter.packets().bumpStateId(player, Math.max(0, clientFloor));
        if (bridged >= 0) {
            session.recordStateId(bridged);
            return bridged;
        }
        return session.nextStateIdAbove(clientFloor);
    }

    private int protocolState(Player player, MenuSession session) {
        int bridged = adapter.packets().bumpStateId(player, session.stateId());
        if (bridged >= 0) {
            session.recordStateId(bridged);
            return bridged;
        }
        return session.nextStateId();
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

    private static final class NewDragList implements Function<UUID, List<AccumulatedDrag>> {
        @Override
        public List<AccumulatedDrag> apply(UUID key) {
            return new ArrayList<>();
        }
    }

    private static final class BukkitTakeablePredicate implements Predicate<UxItem> {
        private final MenuService owner;
        private final Predicate<ItemStack> predicate;

        private BukkitTakeablePredicate(MenuService owner, Predicate<ItemStack> predicate) {
            this.owner = owner;
            this.predicate = predicate;
        }

        @Override
        public boolean test(UxItem ux) {
            if (ux == null || ux.isEmpty()) {
                return false;
            }
            ItemStack stack = owner.adapter.items().toBukkit(ux);
            return stack != null && predicate.test(stack);
        }
    }

    private static final class OpenMenuTask implements Runnable {
        private final MenuService owner;
        private final Player player;
        private final Menu menu;

        private OpenMenuTask(MenuService owner, Player player, Menu menu) {
            this.owner = owner;
            this.player = player;
            this.menu = menu;
        }

        @Override
        public void run() {
            owner.openMenuSync(player, menu);
        }
    }

    private static final class PresentTask implements Runnable {
        private final MenuService owner;
        private final Player player;
        private final Menu menu;

        private PresentTask(MenuService owner, Player player, Menu menu) {
            this.owner = owner;
            this.player = player;
            this.menu = menu;
        }

        @Override
        public void run() {
            UUID pid = id(player);
            if (owner.closingPlayers.contains(pid)) {
                owner.pendingPresent.put(pid, menu);
                return;
            }
            MenuSession existing = owner.sessions.get(pid);
            if (existing != null && existing.phase() == SessionPhase.OPEN) {
                // Same type+mode → differential SetSlots (no close, no onClose refund).
                if (existing.menu().mode() == menu.mode()
                        && existing.menu().type() == menu.type()) {
                    owner.applyMenuDifferential(player, existing, menu);
                    return;
                }
                // Any other change while open (type, size, mode) → OpenScreen only, no CloseWindow.
                owner.replaceMenuInPlace(player, existing, menu);
                return;
            }
            owner.ensurePipeline(player);
            owner.openMenuSync(player, menu);
        }
    }

    private static final class AllocateWindowId implements IntSupplier {
        private final MenuService owner;
        private final Player player;

        private AllocateWindowId(MenuService owner, Player player) {
            this.owner = owner;
            this.player = player;
        }

        @Override
        public int getAsInt() {
            return owner.adapter.packets().allocateWindowId(player);
        }
    }

    private static final class CloseThenStartTask implements Runnable {
        private final MenuService owner;
        private final Player player;
        private final long delay;
        private final Runnable onSettled;

        private CloseThenStartTask(MenuService owner, Player player, long delay, Runnable onSettled) {
            this.owner = owner;
            this.player = player;
            this.delay = delay;
            this.onSettled = onSettled;
        }

        @Override
        public void run() {
            TransitionToken token = owner.beginTransition(player);
            try {
                owner.closeCurrent(player, true, true, GuiCloseReason.API);
            } catch (Throwable error) {
                owner.endTransition(player, token);
                owner.debug(player, "closeThen close failed: " + error.getClass().getSimpleName());
                throw error;
            }
            owner.scheduler.runLaterForPlayer(player, new CloseThenSettled(owner, token, onSettled), delay);
        }
    }

    private static final class CloseThenSettled implements Consumer<Player> {
        private final MenuService owner;
        private final TransitionToken token;
        private final Runnable onSettled;

        private CloseThenSettled(MenuService owner, TransitionToken token, Runnable onSettled) {
            this.owner = owner;
            this.token = token;
            this.onSettled = onSettled;
        }

        @Override
        public void accept(Player settled) {
            owner.endTransition(settled, token);
            if (onSettled == null || !settled.isOnline()) {
                return;
            }
            try {
                onSettled.run();
            } catch (Throwable error) {
                owner.debug(settled, "closeThen onSettled failed: " + error.getClass().getSimpleName());
            }
        }
    }

    private static final class CompleteFuture implements Runnable {
        private final java.util.concurrent.CompletableFuture<Void> future;

        private CompleteFuture(java.util.concurrent.CompletableFuture<Void> future) {
            this.future = future;
        }

        @Override
        public void run() {
            future.complete(null);
        }
    }

    private static final class UpdateTitleTask implements Runnable {
        private final MenuService owner;
        private final Player player;
        private final Component title;

        private UpdateTitleTask(MenuService owner, Player player, Component title) {
            this.owner = owner;
            this.player = player;
            this.title = title;
        }

        @Override
        public void run() {
            MenuSession session = owner.sessions.get(id(player));
            if (session == null || session.phase() != SessionPhase.OPEN) {
                return;
            }
            Component next = title == null ? Component.empty() : title;
            if (next.equals(session.title())) {
                return;
            }
            session.setTitle(next);
            owner.adapter.packets().sendOpenWindow(
                    player,
                    session.windowId(),
                    session.menu().type().id(),
                    next
            );
            int stateId = owner.protocolState(player, session);
            owner.adapter.packets().mirrorTopSlots(player, session.menu().items());
            if (!owner.adapter.packets().sendBoundAuthority(
                    player, stateId, session.menu().mode() != MenuMode.EDITABLE)) {
                owner.adapter.packets().sendWindowItems(
                        player,
                        session.windowId(),
                        stateId,
                        owner.contentsForOpen(player, session.menu()),
                        owner.carriedItem.get(id(player))
                );
            }
        }
    }

    private static final class CloseMenuTask implements Runnable {
        private final MenuService owner;
        private final Player player;

        private CloseMenuTask(MenuService owner, Player player) {
            this.owner = owner;
            this.player = player;
        }

        @Override
        public void run() {
            owner.closeCurrent(player, true, true, GuiCloseReason.API);
            owner.endBookView(player, true);
        }
    }

    private static final class OpenBookTask implements Runnable {
        private final MenuService owner;
        private final Player player;
        private final BookView view;

        private OpenBookTask(MenuService owner, Player player, BookView view) {
            this.owner = owner;
            this.player = player;
            this.view = view;
        }

        @Override
        public void run() {
            owner.openBookSync(player, view);
        }
    }

    private static final class SingleSlotAllow implements Predicate<Integer> {
        private final int slot;

        private SingleSlotAllow(int slot) {
            this.slot = slot;
        }

        @Override
        public boolean test(Integer value) {
            return value == slot;
        }
    }

    private static final class SessionAllowTake implements Predicate<Integer> {
        private final MenuSession session;

        private SessionAllowTake(MenuSession session) {
            this.session = session;
        }

        @Override
        public boolean test(Integer slot) {
            return session.allowsTake(slot);
        }
    }

    private static final class SessionAllowPlace implements Predicate<Integer> {
        private final MenuSession session;

        private SessionAllowPlace(MenuSession session) {
            this.session = session;
        }

        @Override
        public boolean test(Integer slot) {
            return session.allowsPlace(slot);
        }
    }

    private static final class RefreshWindowTask implements Runnable {
        private final MenuService owner;
        private final Player player;

        private RefreshWindowTask(MenuService owner, Player player) {
            this.owner = owner;
            this.player = player;
        }

        @Override
        public void run() {
            MenuSession session = owner.sessions.get(id(player));
            if (session == null) {
                return;
            }
            boolean editable = session.menu().mode() == MenuMode.EDITABLE;
            if (!editable) {
                owner.carriedItem.remove(id(player));
            }
            int stateId = owner.protocolState(player, session);
            owner.adapter.packets().mirrorTopSlots(player, session.menu().items());
            if (owner.adapter.packets().sendBoundAuthority(player, stateId, !editable)) {
                return;
            }
            UxItem cursor = editable
                    ? owner.carriedItem.getOrDefault(id(player), UxItem.EMPTY)
                    : UxItem.EMPTY;
            owner.adapter.packets().sendWindowItems(
                    player,
                    session.windowId(),
                    stateId,
                    owner.fullContents(player, session.menu()),
                    cursor.isEmpty() ? null : cursor
            );
            if (!editable) {
                owner.adapter.packets().sendCursorItem(player, UxItem.EMPTY);
            }
        }
    }

    private static final class UpdateItemTask implements Runnable {
        private final MenuService owner;
        private final Player player;
        private final UxItem item;
        private final int slot;

        private UpdateItemTask(MenuService owner, Player player, UxItem item, int slot) {
            this.owner = owner;
            this.player = player;
            this.item = item;
            this.slot = slot;
        }

        @Override
        public void run() {
            MenuSession session = owner.sessions.get(id(player));
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
            owner.adapter.packets().sendSetSlot(
                    player, session.windowId(), owner.protocolState(player, session), slot, next);
        }
    }

    private static final class UpdateItemsTask implements Runnable {
        private final MenuService owner;
        private final Player player;
        private final Map<Integer, UxItem> newItems;

        private UpdateItemsTask(MenuService owner, Player player, Map<Integer, UxItem> newItems) {
            this.owner = owner;
            this.player = player;
            this.newItems = newItems;
        }

        @Override
        public void run() {
            MenuSession session = owner.sessions.get(id(player));
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
            int stateId = owner.protocolState(player, session);
            for (Map.Entry<Integer, UxItem> entry : dirty.entrySet()) {
                owner.adapter.packets().sendSetSlot(
                        player,
                        session.windowId(),
                        stateId,
                        entry.getKey(),
                        entry.getValue()
                );
            }
        }
    }

    private static final class UpdateButtonTask implements Runnable {
        private final MenuService owner;
        private final Player player;
        private final Button newButton;
        private final int slot;

        private UpdateButtonTask(MenuService owner, Player player, Button newButton, int slot) {
            this.owner = owner;
            this.player = player;
            this.newButton = newButton;
            this.slot = slot;
        }

        @Override
        public void run() {
            MenuSession session = owner.sessions.get(id(player));
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
            owner.adapter.packets().sendSetSlot(
                    player, session.windowId(), owner.protocolState(player, session), slot, next);
        }
    }

    private static final class UpdateButtonsTask implements Runnable {
        private final MenuService owner;
        private final Player player;
        private final Map<Integer, Button> newButtons;

        private UpdateButtonsTask(MenuService owner, Player player, Map<Integer, Button> newButtons) {
            this.owner = owner;
            this.player = player;
            this.newButtons = newButtons;
        }

        @Override
        public void run() {
            MenuSession session = owner.sessions.get(id(player));
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
            int stateId = owner.protocolState(player, session);
            int windowId = session.windowId();
            for (Map.Entry<Integer, UxItem> entry : dirty.entrySet()) {
                owner.adapter.packets().sendSetSlot(
                        player, windowId, stateId, entry.getKey(), entry.getValue());
            }
        }
    }

    private static final class UpdateButtonsBySlotTask implements Runnable {
        private final MenuService owner;
        private final Player player;
        private final Map<Integer, Button> patches;

        private UpdateButtonsBySlotTask(MenuService owner, Player player, Map<Integer, Button> patches) {
            this.owner = owner;
            this.player = player;
            this.patches = patches;
        }

        @Override
        public void run() {
            MenuSession session = owner.sessions.get(id(player));
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
            int stateId = owner.protocolState(player, session);
            for (Map.Entry<Integer, UxItem> entry : dirty.entrySet()) {
                owner.adapter.packets().sendSetSlot(
                        player, session.windowId(), stateId, entry.getKey(), entry.getValue());
            }
        }
    }

    private static final class PatchSlotAtomicTask implements Runnable {
        private final MenuService owner;
        private final Player player;
        private final int slot;
        private final UxItem item;
        private final Consumer<IButtonBuilder> buttonBuilder;
        private final SlotKind slotKind;

        private PatchSlotAtomicTask(
                MenuService owner,
                Player player,
                int slot,
                UxItem item,
                Consumer<IButtonBuilder> buttonBuilder,
                SlotKind slotKind
        ) {
            this.owner = owner;
            this.player = player;
            this.slot = slot;
            this.item = item;
            this.buttonBuilder = buttonBuilder;
            this.slotKind = slotKind;
        }

        @Override
        public void run() {
            MenuSession session = owner.sessions.get(id(player));
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
                button = builder.kind(slotKind == null ? SlotKind.ACTION : slotKind)
                        .item(item == null ? UxItem.EMPTY : item)
                        .build();
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
            owner.adapter.packets().sendSetSlot(
                    player, session.windowId(), owner.protocolState(player, session), slot, next);
        }
    }
}
