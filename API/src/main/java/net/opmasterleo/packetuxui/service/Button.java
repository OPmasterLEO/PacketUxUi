package net.opmasterleo.packetuxui.service;

import java.util.function.Consumer;

import net.opmasterleo.packetuxui.dto.CooldownComponent;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.types.ExecuteComponent;

public final class Button {

    private final UxItem item;
    private final Consumer<ExecuteComponent> execute;
    private final CooldownComponent cooldown;
    private final SlotKind kind;

    public Button(UxItem item, Consumer<ExecuteComponent> execute, CooldownComponent cooldown) {
        this(item, execute, cooldown, SlotKind.ACTION);
    }

    public Button(UxItem item, Consumer<ExecuteComponent> execute, CooldownComponent cooldown, boolean takeable) {
        this(item, execute, cooldown, takeable ? SlotKind.EDITABLE : SlotKind.ACTION);
    }

    public Button(UxItem item, Consumer<ExecuteComponent> execute, CooldownComponent cooldown, SlotKind kind) {
        this.item = item;
        this.execute = execute;
        this.cooldown = cooldown;
        this.kind = kind == null ? SlotKind.ACTION : kind;
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

    public SlotKind kind() {
        return kind;
    }

    public boolean takeable() {
        return kind == SlotKind.EDITABLE;
    }
}
