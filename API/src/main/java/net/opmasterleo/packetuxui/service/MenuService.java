package net.opmasterleo.packetuxui.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.entity.Player;

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

    public static final int WINDOW_ID = 126;

    private final NmsAdapter adapter;
    private final PlatformScheduler scheduler;
    private final ConcurrentHashMap<Player, Menu> viewers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Player, UxItem> carriedItem = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Player, List<AccumulatedDrag>> accumulatedDrag = new ConcurrentHashMap<>();

    public MenuService(NmsAdapter adapter, PlatformScheduler scheduler) {
        this.adapter = adapter;
        this.scheduler = scheduler;
    }

    public void openMenu(Player player, Menu menu) {
        scheduler.runForPlayer(player, () -> {
            Menu copy = menu.copy();
            viewers.put(player, copy);
            adapter.packets().sendOpenWindow(player, WINDOW_ID, copy.type().id(), copy.name());
            adapter.packets().sendWindowItems(player, WINDOW_ID, 0, copy.items(), null);
        });
    }

    public void onCloseMenu(Player player) {
        viewers.remove(player);
        carriedItem.remove(player);
        clearAccumulatedDrag(player);
    }

    public void handleIncomingClick(Player player, ClickPacket packet) {
        ClickData clickData = getClickType(packet);
        if (clickData.clickType() == ClickType.DRAG_START || clickData.clickType() == ClickType.DRAG_ADD) {
            accumulateDrag(player, packet, clickData.clickType());
            return;
        }
        if (isMenuClick(packet, clickData.clickType(), player)) {
            handleClickMenu(player, clickData, packet.slot());
            player.updateInventory();
        } else {
            handleClickInventory(player, packet);
        }
    }

    public void handleClickInventory(Player player, ClickPacket packet) {
        Menu menu = requireMenu(player);
        ClickData clickData = getClickType(packet);
        updateCarriedItem(player, packet.carried(), clickData.clickType());
        if (clickData.clickType() == ClickType.DRAG_END) {
            handleDragEnd(player, menu);
        }
        adapter.packets().injectClick(player, createAdjustedClickPacket(packet, menu));
    }

    public void handleClickMenu(Player player, ClickData clickData, int slot) {
        if (clickData.clickType() == ClickType.DRAG_END) {
            clearAccumulatedDrag(player);
        }
        UxItem carried = carriedItem.get(player);
        Menu menu = requireMenu(player);
        Button button = menu.buttons().get(slot);
        if (button == null) {
            adapter.packets().sendWindowItems(player, WINDOW_ID, 0, menu.items(), carried);
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
        adapter.packets().sendWindowItems(player, WINDOW_ID, 0, menu.items(), carried);
        if (execute != null) {
            execute.accept(new ExecuteComponent(player, clickData.buttonType(), slot, carried));
        }
    }

    public void updateItem(Player player, UxItem item, int slot) {
        Menu menu = getMenu(player);
        if (menu == null) {
            return;
        }
        if (slot > menu.type().lastIndex()) {
            throw new IllegalArgumentException("Slot out of range.");
        }
        List<UxItem> items = new ArrayList<>(menu.items());
        items.set(slot, item);
        menu.setItems(items);
        adapter.packets().sendSetSlot(player, WINDOW_ID, 0, slot, item);
    }

    public void updateItems(Player player, Map<Integer, UxItem> newItems) {
        Menu menu = getMenu(player);
        if (menu == null) {
            return;
        }
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
        for (Map.Entry<Integer, UxItem> entry : newItems.entrySet()) {
            adapter.packets().sendSetSlot(player, WINDOW_ID, 0, entry.getKey(), entry.getValue());
        }
    }

    public void updateButton(Player player, Button newButton, int slot) {
        Menu menu = getMenu(player);
        if (menu == null) {
            return;
        }
        if (slot > menu.type().lastIndex()) {
            throw new IllegalArgumentException("Slot out of range.");
        }
        menu.buttons().put(slot, newButton);
        List<UxItem> items = new ArrayList<>(menu.items());
        items.set(slot, newButton.item());
        menu.setItems(items);
        adapter.packets().sendSetSlot(player, WINDOW_ID, 0, slot, newButton.item());
    }

    public void updateButtons(Player player, Map<Integer, Button> newButtons) {
        Menu menu = getMenu(player);
        if (menu == null) {
            return;
        }
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
        for (Map.Entry<Integer, Button> entry : newButtons.entrySet()) {
            adapter.packets().sendSetSlot(player, WINDOW_ID, 0, entry.getKey(), entry.getValue().item());
        }
    }

    public Menu getMenu(Player player) {
        return viewers.get(player);
    }

    public boolean shouldIgnore(int id, Player player) {
        return id != WINDOW_ID || !viewers.containsKey(player);
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
        Menu menu = viewers.get(player);
        if (menu == null) {
            throw new IllegalStateException("Menu under player key not found.");
        }
        return menu;
    }
}
