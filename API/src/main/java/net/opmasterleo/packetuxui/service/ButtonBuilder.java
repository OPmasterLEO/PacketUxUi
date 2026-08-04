package net.opmasterleo.packetuxui.service;

import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.opmasterleo.packetuxui.dto.CooldownComponent;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.types.ExecuteComponent;

public final class ButtonBuilder implements IButtonBuilder {

    private UxItem item = UxItem.EMPTY;
    private Consumer<ExecuteComponent> click;
    private CooldownComponent cooldown = new CooldownComponent(0);
    private SlotKind kind = SlotKind.ACTION;

    @Override
    public IButtonBuilder item(UxItem item) {
        this.item = item == null ? UxItem.EMPTY : item;
        return this;
    }

    @Override
    public IButtonBuilder click(Consumer<ExecuteComponent> click) {
        this.click = click;
        return this;
    }

    @Override
    public IButtonBuilder executeCommand(String[] command) {
        this.click = new ConsoleCommandClick(command);
        return this;
    }

    @Override
    public IButtonBuilder makePlayerExecuteCommand(String[] command) {
        this.click = new PlayerCommandClick(command);
        return this;
    }

    @Override
    public IButtonBuilder cooldown(CooldownComponent cooldown) {
        this.cooldown = cooldown == null ? new CooldownComponent(0) : cooldown;
        return this;
    }

    @Override
    public IButtonBuilder takeable(boolean takeable) {
        this.kind = takeable ? SlotKind.EDITABLE : SlotKind.ACTION;
        return this;
    }

    @Override
    public IButtonBuilder kind(SlotKind kind) {
        this.kind = kind == null ? SlotKind.ACTION : kind;
        return this;
    }

    @Override
    public IButtonBuilder decorative() {
        return kind(SlotKind.DECORATIVE);
    }

    @Override
    public IButtonBuilder action() {
        return kind(SlotKind.ACTION);
    }

    @Override
    public IButtonBuilder editable() {
        return kind(SlotKind.EDITABLE);
    }

    @Override
    public IButtonBuilder extractable() {
        return kind(SlotKind.EXTRACTABLE);
    }

    @Override
    public Button build() {
        return new Button(item, click, cooldown, kind);
    }

    private static final class ConsoleCommandClick implements Consumer<ExecuteComponent> {
        private final String[] command;

        private ConsoleCommandClick(String[] command) {
            this.command = command;
        }

        @Override
        public void accept(ExecuteComponent component) {
            Player player = component.player();
            for (String raw : command) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String parsed = raw
                        .replace("%player%", player.getName())
                        .replace("%uuid%", player.getUniqueId().toString());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
        }
    }

    private static final class PlayerCommandClick implements Consumer<ExecuteComponent> {
        private final String[] command;

        private PlayerCommandClick(String[] command) {
            this.command = command;
        }

        @Override
        public void accept(ExecuteComponent component) {
            Player player = component.player();
            for (String raw : command) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String parsed = raw
                        .replace("%player%", player.getName())
                        .replace("%uuid%", player.getUniqueId().toString());
                player.performCommand(parsed.startsWith("/") ? parsed.substring(1) : parsed);
            }
        }
    }
}
