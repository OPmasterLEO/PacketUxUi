package net.opmasterleo.packetuxui.service;

import java.util.function.Consumer;

import net.opmasterleo.packetuxui.dto.CooldownComponent;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.types.ExecuteComponent;

public interface IButtonBuilder {

    IButtonBuilder item(UxItem item);

    IButtonBuilder click(Consumer<ExecuteComponent> click);

    IButtonBuilder executeCommand(String[] command);

    IButtonBuilder makePlayerExecuteCommand(String[] command);

    IButtonBuilder cooldown(CooldownComponent cooldown);

    IButtonBuilder takeable(boolean takeable);

    Button build();
}
