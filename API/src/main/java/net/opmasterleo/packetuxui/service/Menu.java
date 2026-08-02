package net.opmasterleo.packetuxui.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.dto.CooldownComponent;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.types.InventoryType;

public class Menu {

    private final Component name;
    private final InventoryType type;
    private final ConcurrentMap<Integer, Button> buttons;
    private final CooldownComponent cooldown;
    private volatile List<UxItem> items;

    public Menu(Component name, InventoryType type, Map<Integer, Button> buttons) {
        this(name, type, buttons, new CooldownComponent());
    }

    public Menu(Component name, InventoryType type, Map<Integer, Button> buttons, CooldownComponent cooldown) {
        if (buttons.size() > type.size()) {
            throw new IllegalArgumentException("Too many items in menu");
        }
        this.name = name;
        this.type = type;
        this.buttons = new ConcurrentHashMap<>(buttons);
        this.cooldown = cooldown;
        List<UxItem> built = new ArrayList<>(type.size());
        for (int index = 0; index < type.size(); index++) {
            Button button = this.buttons.get(index);
            built.add(button != null ? button.item() : UxItem.EMPTY);
        }
        this.items = List.copyOf(built);
    }

    public Component name() {
        return name;
    }

    public InventoryType type() {
        return type;
    }

    public ConcurrentMap<Integer, Button> buttons() {
        return buttons;
    }

    public CooldownComponent cooldown() {
        return cooldown;
    }

    public List<UxItem> items() {
        return items;
    }

    public void setItems(List<UxItem> items) {
        this.items = List.copyOf(items);
    }

    public Menu copy() {
        return new Menu(name, type, buttons, cooldown);
    }
}
