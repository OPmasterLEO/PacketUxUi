package net.opmasterleo.packetuxui.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.PacketUxUiAPI;
import net.opmasterleo.packetuxui.common.StringUtils;
import net.opmasterleo.packetuxui.dto.CooldownComponent;
import net.opmasterleo.packetuxui.types.InventoryType;

public final class MenuBuilder {

    private final Component name;
    private final InventoryType type;
    private final Map<Integer, Button> buttons = new LinkedHashMap<>();
    private CooldownComponent cooldown = new CooldownComponent();
    private MenuMode mode = MenuMode.READ_ONLY;
    private BiConsumer<Player, CloseSnapshot> onClose;

    private MenuBuilder(Component name, InventoryType type) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
    }

    public static MenuBuilder of(Component name, InventoryType type) {
        return new MenuBuilder(name, type);
    }

    public static MenuBuilder of(String miniMessageTitle, InventoryType type) {
        return new MenuBuilder(StringUtils.toComponent(miniMessageTitle), type);
    }

    public MenuBuilder mode(MenuMode mode) {
        this.mode = mode == null ? MenuMode.READ_ONLY : mode;
        return this;
    }

    public MenuBuilder readOnly() {
        return mode(MenuMode.READ_ONLY);
    }

    public MenuBuilder editable() {
        return mode(MenuMode.EDITABLE);
    }

    /** @deprecated use {@link #editable()} */
    @Deprecated
    public MenuBuilder editablePlayerInventory() {
        return mode(MenuMode.EDITABLE_PLAYER_INVENTORY);
    }

    public MenuBuilder onClose(BiConsumer<Player, CloseSnapshot> onClose) {
        this.onClose = onClose;
        return this;
    }

    public MenuBuilder cooldown(CooldownComponent cooldown) {
        this.cooldown = cooldown == null ? new CooldownComponent() : cooldown;
        return this;
    }

    public MenuBuilder button(int slot, Button button) {
        if (slot < 0 || slot >= type.size()) {
            throw new IllegalArgumentException("Slot out of range: " + slot);
        }
        buttons.put(slot, Objects.requireNonNull(button, "button"));
        return this;
    }

    public MenuBuilder button(int slot, Consumer<IButtonBuilder> configurator) {
        ButtonBuilder builder = new ButtonBuilder();
        configurator.accept(builder);
        return button(slot, builder.build());
    }

    public MenuBuilder fill(Button button) {
        Objects.requireNonNull(button, "button");
        for (int slot = 0; slot < type.size(); slot++) {
            buttons.putIfAbsent(slot, button);
        }
        return this;
    }

    public Menu build() {
        return new Menu(name, type, buttons, cooldown, mode, onClose);
    }

    public Menu open(Player player) {
        Menu menu = build();
        PacketUxUiAPI.open(player, menu);
        return menu;
    }
}
