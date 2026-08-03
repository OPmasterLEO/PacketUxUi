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
    private boolean takeable;

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
        this.click = component -> {
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
        };
        return this;
    }

    @Override
    public IButtonBuilder makePlayerExecuteCommand(String[] command) {
        this.click = component -> {
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
        };
        return this;
    }

    @Override
    public IButtonBuilder cooldown(CooldownComponent cooldown) {
        this.cooldown = cooldown == null ? new CooldownComponent(0) : cooldown;
        return this;
    }

    @Override
    public IButtonBuilder takeable(boolean takeable) {
        this.takeable = takeable;
        return this;
    }

    @Override
    public Button build() {
        return new Button(item, click, cooldown, takeable);
    }
}
