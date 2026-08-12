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
import net.opmasterleo.packetuxui.service.BookView;
import net.opmasterleo.packetuxui.service.GuiScopeListener;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.service.MenuService;
import net.opmasterleo.packetuxui.service.MenuSessionDiagnostics;
import net.opmasterleo.packetuxui.service.SessionPhase;
import net.opmasterleo.packetuxui.service.SlotKind;

public final class PacketGuiManager {

    private static volatile PacketGuiManager INSTANCE;

    private final MenuService service;
    private final PlatformScheduler scheduler;
    private final ConcurrentHashMap<UUID, Consumer<Player>> closeHooks = new ConcurrentHashMap<>();
    private volatile Consumer<Player> globalCloseHook;
    private volatile Consumer<Player> defaultClickSound;
    private volatile GuiScopeListener userScopeListener;
    private final GuiScopeListener dispatchScope = new DispatchScope(this);

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

    public void present(Player player, MenuBuild build) {
        present(player, build.materialize());
    }

    public void reopen(Player player, Menu menu) {
        service.reopen(player, menu);
    }

    public void reopen(Player player, MenuBuild build) {
        reopen(player, build.materialize());
    }

    /** Open a written-book text GUI (see {@link BookView} limits). */
    public void openBook(Player player, BookView view) {
        service.openBook(player, view);
    }

    public void openBook(Player player, BookBuild build) {
        openBook(player, build.build());
    }

    public void openSign(Player player, net.opmasterleo.packetuxui.service.SignView view) {
        service.openSign(player, view);
    }

    public void openSign(Player player, SignBuild build) {
        openSign(player, build.build());
    }

    public boolean hasSignOpen(Player player) {
        return service.hasSignOpen(player);
    }

    public boolean hasBookOpen(Player player) {
        return service.hasBookOpen(player);
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

    /**
     * Build on the dedicated menu worker pool, preload item bridges, then
     * {@link #present} on the player/entity scheduler (Folia-safe).
     */
    public void presentAsync(Player player, Supplier<MenuBuild> builder) {
        presentAsync(player, builder, null);
    }

    public void presentAsync(Player player, Supplier<MenuBuild> builder, Consumer<Throwable> onError) {
        Objects.requireNonNull(builder, "builder");
        if (!scheduler.menuWorkers().isAvailable()) {
            if (onError != null) {
                onError.accept(new IllegalStateException("menu worker pool closed"));
            }
            return;
        }
        scheduler.runMenuAsync(new PresentAsyncBuildTask(this, player, builder, onError));
    }

    public CompletableFuture<Void> presentAsyncFuture(Player player, Supplier<MenuBuild> builder) {
        Objects.requireNonNull(builder, "builder");
        if (!scheduler.menuWorkers().isAvailable()) {
            return CompletableFuture.failedFuture(new IllegalStateException("menu worker pool closed"));
        }
        CompletableFuture<Menu> built = scheduler.supplyMenuAsync(new PresentAsyncSupply(builder));
        return built.thenCompose(new PresentAsyncCompose(this, player));
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

    /** Hard-clear stranded session/transition state for a player. */
    public void resetPlayer(Player player) {
        service.resetPlayer(player);
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
        this.userScopeListener = new OpenCloseScopeAdapter(openClose);
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

    void dispatchScope(Player player, boolean open, int topSlotCount) {
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

    private static final class DispatchScope implements GuiScopeListener {
        private final PacketGuiManager owner;

        private DispatchScope(PacketGuiManager owner) {
            this.owner = owner;
        }

        @Override
        public void onScopeChanged(Player player, boolean open, int topSlotCount) {
            owner.dispatchScope(player, open, topSlotCount);
        }
    }

    private static final class OpenCloseScopeAdapter implements GuiScopeListener {
        private final BiConsumer<Player, Boolean> openClose;

        private OpenCloseScopeAdapter(BiConsumer<Player, Boolean> openClose) {
            this.openClose = openClose;
        }

        @Override
        public void onScopeChanged(Player player, boolean open, int topSlotCount) {
            openClose.accept(player, open);
        }
    }

    private static final class PresentAsyncBuildTask implements Runnable {
        private final PacketGuiManager owner;
        private final Player player;
        private final Supplier<MenuBuild> builder;
        private final Consumer<Throwable> onError;

        private PresentAsyncBuildTask(
                PacketGuiManager owner,
                Player player,
                Supplier<MenuBuild> builder,
                Consumer<Throwable> onError
        ) {
            this.owner = owner;
            this.player = player;
            this.builder = builder;
            this.onError = onError;
        }

        @Override
        public void run() {
            if (!owner.scheduler.menuWorkers().isAvailable()) {
                return;
            }
            Menu menu;
            try {
                MenuBuild build = builder.get();
                if (build == null) {
                    return;
                }
                menu = build.materialize();
                PacketUxUiAPI.getAdapter().items().preload(menu.items());
            } catch (Throwable error) {
                if (onError != null) {
                    onError.accept(error);
                }
                return;
            }
            if (!owner.scheduler.menuWorkers().isAvailable()) {
                return;
            }
            owner.scheduler.runForPlayer(player, new PresentOnlineTask(owner, player, menu));
        }
    }

    private static final class PresentOnlineTask implements Runnable {
        private final PacketGuiManager owner;
        private final Player player;
        private final Menu menu;

        private PresentOnlineTask(PacketGuiManager owner, Player player, Menu menu) {
            this.owner = owner;
            this.player = player;
            this.menu = menu;
        }

        @Override
        public void run() {
            if (player.isOnline()) {
                owner.present(player, menu);
            }
        }
    }

    private static final class PresentAsyncSupply implements Supplier<Menu> {
        private final Supplier<MenuBuild> builder;

        private PresentAsyncSupply(Supplier<MenuBuild> builder) {
            this.builder = builder;
        }

        @Override
        public Menu get() {
            MenuBuild build = builder.get();
            if (build == null) {
                throw new IllegalStateException("builder returned null");
            }
            Menu menu = build.materialize();
            PacketUxUiAPI.getAdapter().items().preload(menu.items());
            return menu;
        }
    }

    private static final class PresentAsyncCompose
            implements java.util.function.Function<Menu, CompletableFuture<Void>> {
        private final PacketGuiManager owner;
        private final Player player;

        private PresentAsyncCompose(PacketGuiManager owner, Player player) {
            this.owner = owner;
            this.player = player;
        }

        @Override
        public CompletableFuture<Void> apply(Menu menu) {
            CompletableFuture<Void> done = new CompletableFuture<>();
            owner.scheduler.runForPlayer(
                    player,
                    new PresentFutureTask(owner, player, menu, done),
                    new PresentRetiredTask(done)
            );
            return done;
        }
    }

    private static final class PresentFutureTask implements Runnable {
        private final PacketGuiManager owner;
        private final Player player;
        private final Menu menu;
        private final CompletableFuture<Void> done;

        private PresentFutureTask(
                PacketGuiManager owner,
                Player player,
                Menu menu,
                CompletableFuture<Void> done
        ) {
            this.owner = owner;
            this.player = player;
            this.menu = menu;
            this.done = done;
        }

        @Override
        public void run() {
            if (!player.isOnline()) {
                done.completeExceptionally(new IllegalStateException("player offline"));
                return;
            }
            try {
                owner.present(player, menu);
                done.complete(null);
            } catch (Throwable error) {
                done.completeExceptionally(error);
            }
        }
    }

    private static final class PresentRetiredTask implements Runnable {
        private final CompletableFuture<Void> done;

        private PresentRetiredTask(CompletableFuture<Void> done) {
            this.done = done;
        }

        @Override
        public void run() {
            done.completeExceptionally(new IllegalStateException("player retired"));
        }
    }
}
