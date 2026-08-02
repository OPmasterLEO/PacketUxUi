package net.opmasterleo.packetuxui.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.entity.Player;

import net.opmasterleo.packetuxui.dto.AccumulatedDrag;
import net.opmasterleo.packetuxui.dto.CooldownComponent;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.NmsAdapter;
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
    private final ConcurrentHashMap<Player, MenuSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Player, UxItem> carriedItem = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Player, List<AccumulatedDrag>> accumulatedDrag = new ConcurrentHashMap<>();

    public MenuService(NmsAdapter adapter, PlatformScheduler scheduler) {
        this.adapter = adapter;
        this.scheduler = scheduler;
    }

    public void openMenu(Player player, Menu menu) {
        scheduler.runForPlayer(player, () -> {
            closeCurrent(player, true);
            Menu copy = menu.copy();
            int windowId = windowIds.allocate(player);
            MenuSession session = new MenuSession(copy, windowId);
            sessions.put(player, session);
            int stateId = session.nextStateId();
            adapter.packets().sendOpenWindow(player, windowId, copy.type().id(), copy.name());
            adapter.packets().sendWindowItems(player, windowId, stateId, copy.items(), null);
        });
    }

    public void onCloseMenu(Player player) {
        closeCurrent(player, false);
    }

    public void closeMenu(Player player) {
        scheduler.runForPlayer(player, () -> closeCurrent(player, true));
    }

    private void closeCurrent(Player player, boolean sendClosePacket) {
        MenuSession session = sessions.get(player);
        if (session != null && sendClosePacket) {
            adapter.packets().sendCloseWindow(player, session.windowId());
        }
        sessions.remove(player);
        windowIds.release(player);
        carriedItem.remove(player);
        clearAccumulatedDrag(player);
    }

    public void handleIncomingClick(Player player, ClickPacket packet) {
        ClickData clickData = getClickType(packet);
        if (clickData.clickType() == ClickType.DRAG_START || clickData.clickType() == ClickType.DRAG_ADD) {
            accumulateDrag(player, packet, clickData.clickType());
            return;
        }
        MenuSession session = sessions.get(player);
        if (session == null) {
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
        resyncDirtySlots(player, session, packet, UxItem.EMPTY);
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
        // Always revert client prediction with dirty Set Slots — never full content
        // and never player.updateInventory().
        resyncDirtySlots(player, session, packet, UxItem.EMPTY);
        carriedItem.remove(player);
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
            MenuSession session = sessions.get(player);
            if (session == null) {
                return;
            }
            int stateId = session.nextStateId();
            adapter.packets().sendWindowItems(
                    player,
                    session.windowId(),
                    stateId,
                    session.menu().items(),
                    carriedItem.get(player)
            );
        });
    }

    public void updateItem(Player player, UxItem item, int slot) {
        scheduler.runForPlayer(player, () -> {
            MenuSession session = sessions.get(player);
            if (session == null) {
                return;
            }
            Menu menu = session.menu();
            if (slot > menu.type().lastIndex()) {
                throw new IllegalArgumentException("Slot out of range.");
            }
            List<UxItem> items = new ArrayList<>(menu.items());
            items.set(slot, item);
            menu.setItems(items);
            adapter.packets().sendSetSlot(player, session.windowId(), session.nextStateId(), slot, item);
        });
    }

    public void updateItems(Player player, Map<Integer, UxItem> newItems) {
        scheduler.runForPlayer(player, () -> {
            MenuSession session = sessions.get(player);
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
            for (Map.Entry<Integer, UxItem> entry : newItems.entrySet()) {
                items.set(entry.getKey(), entry.getValue());
            }
            menu.setItems(items);
            int stateId = session.nextStateId();
            for (Map.Entry<Integer, UxItem> entry : newItems.entrySet()) {
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
            MenuSession session = sessions.get(player);
            if (session == null) {
                return;
            }
            Menu menu = session.menu();
            if (slot > menu.type().lastIndex()) {
                throw new IllegalArgumentException("Slot out of range.");
            }
            menu.buttons().put(slot, newButton);
            List<UxItem> items = new ArrayList<>(menu.items());
            items.set(slot, newButton.item());
            menu.setItems(items);
            adapter.packets().sendSetSlot(player, session.windowId(), session.nextStateId(), slot, newButton.item());
        });
    }

    public void updateButtons(Player player, Map<Integer, Button> newButtons) {
        scheduler.runForPlayer(player, () -> {
            MenuSession session = sessions.get(player);
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
            for (int index = 0; index < menu.type().size(); index++) {
                Button button = newButtons.get(index);
                items.add(button != null ? button.item() : UxItem.EMPTY);
            }
            menu.setItems(items);
            int stateId = session.nextStateId();
            int windowId = session.windowId();
            for (int index = 0; index < items.size(); index++) {
                adapter.packets().sendSetSlot(player, windowId, stateId, index, items.get(index));
            }
        });
    }

    public Menu getMenu(Player player) {
        MenuSession session = sessions.get(player);
        return session == null ? null : session.menu();
    }

    public MenuSession getSession(Player player) {
        return sessions.get(player);
    }

    public boolean shouldIgnore(int id, Player player) {
        MenuSession session = sessions.get(player);
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
        accumulatedDrag.computeIfAbsent(player, key -> new ArrayList<>()).add(new AccumulatedDrag(packet, type));
    }

    private void handleDragEnd(Player player, Menu menu) {
        List<AccumulatedDrag> drags = accumulatedDrag.get(player);
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
        List<AccumulatedDrag> drags = accumulatedDrag.get(player);
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
                net.opmasterleo.packetuxui.nms.WindowClickType.PICKUP,
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
            carriedItem.remove(player);
            return;
        }
        switch (clickType) {
            case PICKUP, PICKUP_ALL, DRAG_START, DRAG_END -> carriedItem.put(player, carried);
            default -> carriedItem.remove(player);
        }
    }

    private Menu requireMenu(Player player) {
        return requireSession(player).menu();
    }

    private MenuSession requireSession(Player player) {
        MenuSession session = sessions.get(player);
        if (session == null) {
            throw new IllegalStateException("Menu under player key not found.");
        }
        return session;
    }
}
