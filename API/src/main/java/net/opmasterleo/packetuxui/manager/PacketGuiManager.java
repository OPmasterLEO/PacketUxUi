package net.opmasterleo.packetuxui.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.PacketUxUiAPI;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.service.GuiScopeListener;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.service.MenuService;
import net.opmasterleo.packetuxui.service.MenuSessionDiagnostics;
import net.opmasterleo.packetuxui.service.MenuSession;
import net.opmasterleo.packetuxui.service.SlotKind;
import net.opmasterleo.packetuxui.service.SessionPhase;
import net.opmasterleo.packetuxui.service.TransitionToken;

public final class PacketGuiManager {

    private static volatile PacketGuiManager INSTANCE;

    private final MenuService service;
    private final PlatformScheduler scheduler;
    private final ConcurrentHashMap<UUID, Consumer<Player>> closeHooks = new ConcurrentHashMap<>();
    private volatile Consumer<Player> globalCloseHook;
    private volatile Consumer<Player> defaultClickSound;
    private volatile GuiScopeListener userScopeListener;
    private final GuiScopeListener dispatchScope = this::dispatchScope;

    public PacketGuiManager(MenuService service, PlatformScheduler scheduler) {
        this.service = Objects.requireNonNull(service, "service");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.service.setScopeListener(dispatchScope);
    }

    public static PacketGuiManager ofApi() {
        PacketGuiManager local = INSTANCE;
        if (local == null) {
            synchronized (PacketGuiManager.class) {
                local = INSTANCE;
                if (local == null) {
                    local = new PacketGuiManager(PacketUxUiAPI.getService(), PacketUxUiAPI.getScheduler());
                    INSTANCE = local;
                }
            }
        }
        return local;
    }

    public static void resetHolder() {
        INSTANCE = null;
    }

    public MenuService service() {
        return service;
    }

    public void open(Player player, Menu menu) {
        service.openMenu(player, menu);
    }

    public void open(Player player, MenuBuild build) {
        open(player, build.materialize());
    }

    public void close(Player player) {
        service.closeMenu(player);
    }

    /**
     * Close the packet menu, wait one tick (cursor empty / unbound / session gone), then run
     * {@code onSettled}. Prefer this over delayed Bukkit tasks before SignGUI / chat UIs.
     */
    public void closeThen(Player player, Runnable onSettled) {
        service.closeThen(player, onSettled);
    }

    public void closeThen(Player player, long settleTicks, Runnable onSettled) {
        service.closeThen(player, settleTicks, onSettled);
    }

    public CompletableFuture<Void> closeAsync(Player player) {
        return service.closeAsync(player);
    }

    public CompletableFuture<Void> closeAsync(Player player, long settleTicks) {
        return service.closeAsync(player, settleTicks);
    }

    public void present(Player player, Menu menu) {
        service.present(player, menu);
    }

    public void present(Player player, Menu menu, PresentOptions options) {
        if (options != null && options.forceReopenIfOnCloseChanged()) {
            Menu current = service.getMenu(player);
            if (current != null && current.onClose() != menu.onClose()) {
                service.openMenu(player, menu);
                return;
            }
        }
        service.present(player, menu);
    }

    public void reopen(Player player, Menu menu) {
        service.reopen(player, menu);
    }

    public void reopen(Player player, MenuBuild build) {
        reopen(player, build.materialize());
    }

    public void present(Player player, MenuBuild build) {
        present(player, build.materialize());
    }

    public void present(Player player, MenuBuild build, PresentOptions options) {
        present(player, build.materialize(), options);
    }

    public void update(Player player, Menu menu) {
        present(player, menu);
    }

    public void update(Player player, Menu menu, PresentOptions options) {
        present(player, menu, options);
    }

    public void update(Player player, MenuBuild build) {
        present(player, build);
    }

    public void update(Player player, MenuBuild build, PresentOptions options) {
        present(player, build, options);
    }

    public TransitionToken beginTransition(Player player) {
        return service.beginTransition(player);
    }

    public boolean endTransition(Player player, TransitionToken token) {
        return service.endTransition(player, token);
    }

    public void patchSlots(Player player, Map<Integer, ItemStack> slots) {
        if (slots == null || slots.isEmpty()) {
            return;
        }
        Map<Integer, UxItem> ux = new HashMap<>();
        for (Map.Entry<Integer, ItemStack> entry : slots.entrySet()) {
            ItemStack stack = entry.getValue();
            ux.put(
                    entry.getKey(),
                    stack == null
                            ? UxItem.EMPTY
                            : PacketUxUiAPI.getAdapter().items().fromBukkit(stack)
            );
        }
        service.updateItems(player, ux);
    }

    public void updateButtons(Player player, Map<Integer, net.opmasterleo.packetuxui.service.Button> buttonPatches) {
        service.updateButtonsBySlot(player, buttonPatches);
    }

    public void clearButtons(Player player, java.util.Set<Integer> slots) {
        service.clearButtons(player, slots);
    }

    public void patchSlotAtomic(
            Player player,
            int slot,
            ItemStack item,
            Consumer<net.opmasterleo.packetuxui.service.IButtonBuilder> buttonBuilder,
            SlotKind slotKind
    ) {
        UxItem ux = item == null ? UxItem.EMPTY : PacketUxUiAPI.getAdapter().items().fromBukkit(item);
        service.patchSlotAtomic(player, slot, ux, buttonBuilder, slotKind);
    }

    public void refresh(Player player) {
        service.refreshWindow(player);
    }

    public void updateTitle(Player player, Component title) {
        service.updateTitle(player, title);
    }

    public void presentAsync(Player player, Supplier<MenuBuild> builder) {
        presentAsyncResult(player, builder, PresentMode.PRESENT, PresentOptions.DEFAULT);
    }

    public CompletableFuture<AsyncMenuResult> presentAsyncResult(Player player, Supplier<MenuBuild> builder) {
        return presentAsyncResult(player, builder, PresentMode.PRESENT, PresentOptions.DEFAULT);
    }

    public CompletableFuture<AsyncMenuResult> presentAsyncResult(
            Player player,
            Supplier<MenuBuild> builder,
            PresentMode mode,
            PresentOptions options
    ) {
        Objects.requireNonNull(builder, "builder");
        MenuSession prior = service.getSession(player);
        int expected = prior == null ? -1 : prior.generation();
        CompletableFuture<AsyncMenuResult> future = new CompletableFuture<>();
        scheduler.runAsync(() -> {
            MenuBuild build;
            try {
                build = builder.get();
            } catch (Throwable error) {
                future.complete(AsyncMenuResult.fail(
                        AsyncMenuStatus.SUPPLIER_FAILED,
                        error.getClass().getSimpleName(),
                        expected,
                        prior == null ? -1 : prior.generation()
                ));
                return;
            }
            if (build == null) {
                future.complete(AsyncMenuResult.fail(AsyncMenuStatus.SUPPLIER_RETURNED_NULL, "builder returned null", expected, -1));
                return;
            }
            Menu menu = build.materialize();
            scheduler.runForPlayer(player, () -> {
                if (!player.isOnline()) {
                    future.complete(AsyncMenuResult.fail(AsyncMenuStatus.PLAYER_OFFLINE, "player offline", expected, -1));
                    return;
                }
                MenuSession current = service.getSession(player);
                int observed = current == null ? -1 : current.generation();
                if (expected >= 0) {
                    if (current == null || current.generation() != expected || current.phase() != SessionPhase.OPEN) {
                        future.complete(AsyncMenuResult.fail(
                                current == null ? AsyncMenuStatus.MENU_NOT_OPEN : AsyncMenuStatus.GENERATION_MISMATCH,
                                "session changed before apply",
                                expected,
                                observed
                        ));
                        return;
                    }
                }
                switch (mode == null ? PresentMode.PRESENT : mode) {
                    case UPDATE_IF_OPEN -> {
                        if (current == null || current.phase() != SessionPhase.OPEN) {
                            future.complete(AsyncMenuResult.fail(AsyncMenuStatus.MENU_NOT_OPEN, "menu not open", expected, observed));
                            return;
                        }
                        present(player, menu, options == null ? PresentOptions.DEFAULT : options);
                    }
                    case OPEN_IF_MISSING -> {
                        if (current == null) {
                            open(player, menu);
                        } else {
                            present(player, menu, options == null ? PresentOptions.DEFAULT : options);
                        }
                    }
                    default -> present(player, menu, options == null ? PresentOptions.DEFAULT : options);
                }
                MenuSession applied = service.getSession(player);
                if (applied == null || applied.phase() != SessionPhase.OPEN) {
                    future.complete(AsyncMenuResult.fail(AsyncMenuStatus.PHASE_MISMATCH, "session not open after apply", expected, observed));
                    return;
                }
                future.complete(AsyncMenuResult.ok(expected, applied.generation()));
            });
        });
        return future;
    }

    public void updateAsync(Player player, Supplier<MenuBuild> builder) {
        presentAsyncResult(player, builder, PresentMode.UPDATE_IF_OPEN, PresentOptions.DEFAULT);
    }

    public CompletableFuture<AsyncMenuResult> updateAsyncResult(Player player, Supplier<MenuBuild> builder) {
        return presentAsyncResult(player, builder, PresentMode.UPDATE_IF_OPEN, PresentOptions.DEFAULT);
    }

    public void presentAsync(Player player, Supplier<MenuBuild> builder, Consumer<AsyncMenuResult> completion) {
        CompletableFuture<AsyncMenuResult> future = presentAsyncResult(player, builder);
        if (completion != null) {
            future.whenComplete((result, error) -> completion.accept(
                    error == null
                            ? result
                            : AsyncMenuResult.fail(AsyncMenuStatus.SUPPLIER_FAILED, error.getClass().getSimpleName(), -1, -1)
            ));
        }
    }

    public void updateAsync(Player player, Supplier<MenuBuild> builder, Consumer<AsyncMenuResult> completion) {
        CompletableFuture<AsyncMenuResult> future = updateAsyncResult(player, builder);
        if (completion != null) {
            future.whenComplete((result, error) -> completion.accept(
                    error == null
                            ? result
                            : AsyncMenuResult.fail(AsyncMenuStatus.SUPPLIER_FAILED, error.getClass().getSimpleName(), -1, -1)
            ));
        }
    }

    public CompletableFuture<AsyncMenuResult> updateIfOpen(Player player, Supplier<MenuBuild> builder) {
        return presentAsyncResult(player, builder, PresentMode.UPDATE_IF_OPEN, PresentOptions.DEFAULT);
    }

    public CompletableFuture<AsyncMenuResult> presentOrUpdate(
            Player player,
            Supplier<MenuBuild> builder,
            PresentMode mode,
            PresentOptions options
    ) {
        return presentAsyncResult(player, builder, mode, options);
    }

    public void refreshPage(Player player, Supplier<MenuBuild> pageModel, RefreshStrategy strategy) {
        Objects.requireNonNull(pageModel, "pageModel");
        MenuBuild build = pageModel.get();
        if (build == null) {
            return;
        }
        if (strategy == RefreshStrategy.FORCE_REOPEN) {
            service.openMenu(player, build.materialize());
            return;
        }
        present(player, build.materialize());
    }

    public Menu getOpen(Player player) {
        return service.getMenu(player);
    }

    public boolean hasOpen(UUID playerId) {
        return service.hasOpen(playerId);
    }

    public boolean hasOpen(Player player) {
        return player != null && hasOpen(player.getUniqueId());
    }

    public SessionPhase phase(Player player) {
        return service.phase(player);
    }

    public int getWindowId(Player player) {
        return service.getWindowId(player);
    }

    public int getTopSlotCount(Player player) {
        return service.getTopSlotCount(player);
    }

    public MenuSessionDiagnostics diagnostics(Player player) {
        return service.diagnostics(player);
    }

    public String diagnosticsDump(Player player) {
        MenuSessionDiagnostics d = diagnostics(player);
        return "player=" + d.playerId()
                + ", generation=" + d.generation()
                + ", phase=" + d.phase()
                + ", windowId=" + d.windowId()
                + ", title=" + d.title()
                + ", transitionActive=" + d.transitionActive()
                + ", lastClickDecision=" + d.lastClickDecision()
                + ", pipelineHandlers=" + PacketUxUiAPI.getPipelineHandlers(player);
    }

    public java.util.List<String> pipelineHandlers(Player player) {
        return PacketUxUiAPI.getPipelineHandlers(player);
    }

    public void onClose(Player player, Consumer<Player> hook) {
        if (player == null) {
            return;
        }
        if (hook == null) {
            closeHooks.remove(player.getUniqueId());
        } else {
            closeHooks.put(player.getUniqueId(), hook);
        }
    }

    public void onClose(Consumer<Player> globalHook) {
        this.globalCloseHook = globalHook;
    }

    public void setScopeListener(GuiScopeListener listener) {
        this.userScopeListener = listener;
    }

    public void setScopeListener(BiConsumer<Player, Boolean> openClose) {
        if (openClose == null) {
            this.userScopeListener = null;
            return;
        }
        this.userScopeListener = (player, open, top) -> openClose.accept(player, open);
    }

    public void setClickDebounceMillis(long millis) {
        service.setClickDebounceMillis(millis);
    }

    public void setDefaultClickSound(Consumer<Player> sound) {
        this.defaultClickSound = sound;
    }

    public Consumer<Player> defaultClickSound() {
        return defaultClickSound;
    }

    private void dispatchScope(Player player, boolean open, int topSlotCount) {
        if (!open) {
            Consumer<Player> hook = closeHooks.remove(player.getUniqueId());
            if (hook != null) {
                try {
                    hook.accept(player);
                } catch (Throwable ignored) {
                }
            }
            Consumer<Player> global = globalCloseHook;
            if (global != null) {
                try {
                    global.accept(player);
                } catch (Throwable ignored) {
                }
            }
        }
        GuiScopeListener user = userScopeListener;
        if (user != null) {
            try {
                user.onScopeChanged(player, open, topSlotCount);
            } catch (Throwable ignored) {
            }
        }
    }
}
