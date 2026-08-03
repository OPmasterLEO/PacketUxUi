package net.opmasterleo.packetuxui.test.menus;

import java.util.Map;

import net.opmasterleo.packetuxui.common.StringUtils;
import net.opmasterleo.packetuxui.dto.CooldownComponent;
import net.opmasterleo.packetuxui.nms.item.UxItemBuilder;
import net.opmasterleo.packetuxui.service.Button;
import net.opmasterleo.packetuxui.service.ButtonBuilder;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.types.ExecuteComponent;
import net.opmasterleo.packetuxui.types.InventoryType;

public final class CooldownTest {

    private static final String COOLDOWN_HINT = """
            
            <red>You can do everything the same as in execute, 
            when the player is under cooldown
            but not when freeze time""";

    private final Menu menu;

    public CooldownTest() {
        var item = new UxItemBuilder()
                .material("minecraft:diamond_sword")
                .lore(
                        StringUtils.toComponent("<gray>cooldown 10s"),
                        StringUtils.toComponent("<gray>no freeze")
                )
                .amount(1)
                .build();

        var item1 = new UxItemBuilder()
                .material("minecraft:bow")
                .name(StringUtils.toComponent("<gradient:#96d9be:#00b4d8><bold>Test"))
                .lore(
                        StringUtils.toComponent("<gray>cooldown 2s"),
                        StringUtils.toComponent("<gray>freeze 1s")
                )
                .amount(1)
                .build();

        Button button = new ButtonBuilder()
                .item(item)
                .cooldown(new CooldownComponent(10000, CooldownHint.INSTANCE))
                .build();

        Button button1 = new ButtonBuilder()
                .item(item1)
                .cooldown(new CooldownComponent(2000, CooldownHint.INSTANCE, 1000))
                .build();

        this.menu = new Menu(
                StringUtils.toComponent("<gradient:#96d9be:#00b4d8><bold>CooldownTest"),
                InventoryType.ANVIL,
                Map.of(
                        0, button,
                        1, button1,
                        2, button
                )
        );
    }

    public Menu menu() {
        return menu;
    }

    private static final class CooldownHint implements ExecuteComponent.Handler {
        private static final CooldownHint INSTANCE = new CooldownHint();

        @Override
        public void accept(ExecuteComponent it) {
            it.player().sendMessage(StringUtils.toComponent(COOLDOWN_HINT));
        }
    }
}
