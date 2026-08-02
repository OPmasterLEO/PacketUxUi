package net.opmasterleo.packetuxui.service;

import java.util.function.Consumer;

import net.opmasterleo.packetuxui.dto.CooldownComponent;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.types.ExecuteComponent;

public final class Button {

    private final UxItem item;
    private final Consumer<ExecuteComponent> execute;
    private final CooldownComponent cooldown;

    public Button(UxItem item, Consumer<ExecuteComponent> execute, CooldownComponent cooldown) {
        this.item = item;
        this.execute = execute;
        this.cooldown = cooldown;
    }

    public static IButtonBuilder builder() {
        return new ButtonBuilder();
    }

    public UxItem item() {
        return item;
    }

    public Consumer<ExecuteComponent> execute() {
        return execute;
    }

    public CooldownComponent cooldown() {
        return cooldown;
    }
}
