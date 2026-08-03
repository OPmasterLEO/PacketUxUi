package net.opmasterleo.packetuxui.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
import net.opmasterleo.packetuxui.service.MenuSession;
import net.opmasterleo.packetuxui.service.SessionPhase;

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

    public void present(Player player, Menu menu) {
        service.present(player, menu);
    }

    public void present(Player player, MenuBuild build) {
        present(player, build.materialize());
    }

    public void update(Player player, Menu menu) {
        present(player, menu);
    }

    public void update(Player player, MenuBuild build) {
        present(player, build);
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

    public void refresh(Player player) {
        service.refreshWindow(player);
    }

    public void updateTitle(Player player, Component title) {
        service.updateTitle(player, title);
    }

    public void presentAsync(Player player, Supplier<MenuBuild> builder) {
        Objects.requireNonNull(builder, "builder");
        MenuSession prior = service.getSession(player);
        int expected = prior == null ? -1 : prior.generation();
        scheduler.runAsync(() -> {
            MenuBuild build;
            try {
                build = builder.get();
            } catch (Throwable ignored) {
                return;
            }
            if (build == null) {
                return;
            }
            Menu menu = build.materialize();
            scheduler.runForPlayer(player, () -> {
                if (expected >= 0) {
                    MenuSession current = service.getSession(player);
                    if (current == null || current.generation() != expected || current.phase() != SessionPhase.OPEN) {
                        return;
                    }
                }
                present(player, menu);
            });
        });
    }

    public void updateAsync(Player player, Supplier<MenuBuild> builder) {
        Objects.requireNonNull(builder, "builder");
        MenuSession prior = service.getSession(player);
        if (prior == null) {
            presentAsync(player, builder);
            return;
        }
        int expected = prior.generation();
        scheduler.runAsync(() -> {
            MenuBuild build;
            try {
                build = builder.get();
            } catch (Throwable ignored) {
                return;
            }
            if (build == null) {
                return;
            }
            Menu menu = build.materialize();
            scheduler.runForPlayer(player, () -> {
                MenuSession current = service.getSession(player);
                if (current == null || current.generation() != expected || current.phase() != SessionPhase.OPEN) {
                    return;
                }
                present(player, menu);
            });
        });
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
