package net.opmasterleo.packetuxui.service;

import java.util.function.Consumer;

import net.opmasterleo.packetuxui.dto.CooldownComponent;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.types.ExecuteComponent;

public final class ButtonBuilder implements IButtonBuilder {

    private UxItem item = UxItem.EMPTY;
    private Consumer<ExecuteComponent> click;
    private CooldownComponent cooldown = new CooldownComponent(0);

    @Override
    public IButtonBuilder item(UxItem item) {
        this.item = item;
        return this;
    }

    @Override
    public IButtonBuilder click(Consumer<ExecuteComponent> click) {
        this.click = click;
        return this;
    }

    @Override
    public IButtonBuilder executeCommand(String[] command) {
        return this;
    }

    @Override
    public IButtonBuilder makePlayerExecuteCommand(String[] command) {
        return this;
    }

    @Override
    public IButtonBuilder cooldown(CooldownComponent cooldown) {
        this.cooldown = cooldown;
        return this;
    }

    @Override
    public Button build() {
        return new Button(item, click, cooldown);
    }
}
