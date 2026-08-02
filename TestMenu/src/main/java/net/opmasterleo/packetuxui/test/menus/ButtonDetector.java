package net.opmasterleo.packetuxui.test.menus;

import java.util.Map;

import net.opmasterleo.packetuxui.common.StringUtils;
import net.opmasterleo.packetuxui.dto.CooldownComponent;
import net.opmasterleo.packetuxui.nms.item.UxItemBuilder;
import net.opmasterleo.packetuxui.service.Button;
import net.opmasterleo.packetuxui.service.ButtonBuilder;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.types.InventoryType;

public final class ButtonDetector {

    private final Menu menu;

    public ButtonDetector() {
        var glowingItem = new UxItemBuilder()
                .material("minecraft:air")
                .name(StringUtils.toComponent("<yellow><bold>Glowing Stone"))
                .amount(1)
                .build();

        var heavyItem = new UxItemBuilder()
                .material("minecraft:anvil")
                .name(StringUtils.toComponent("<dark_gray><bold>Heavy Anvil"))
                .lore(
                        StringUtils.toComponent("<gray>This item is too heavy to carry."),
                        StringUtils.toComponent("<red>Custom material property: <bold>Heavy")
                )
                .amount(1)
                .build();

        Button hoverButton = new ButtonBuilder()
                .item(glowingItem)
                .click(it -> {
                    it.player().sendMessage(StringUtils.toComponent("<green>You clicked on the glowing button!"));
                    it.player().sendMessage(StringUtils.toComponent("Ur button is " + it.buttonType()));
                })
                .build();

        Button cooldownButton = new ButtonBuilder()
                .item(heavyItem)
                .click(it -> {
                    it.player().sendMessage(StringUtils.toComponent("<gold>Clicked on Heavy Anvil!"));
                    it.player().sendMessage(StringUtils.toComponent("Ur button is " + it.buttonType()));
                })
                .build();

        this.menu = new Menu(
                StringUtils.toComponent("<gradient:#ff7f50:#ff4500><bold>Main Menu"),
                InventoryType.CRAFTING_TABLE,
                Map.of(
                        0, hoverButton,
                        1, cooldownButton,
                        2, hoverButton,
                        3, cooldownButton,
                        4, hoverButton,
                        5, cooldownButton,
                        6, hoverButton,
                        7, cooldownButton,
                        8, hoverButton,
                        9, cooldownButton
                ),
                new CooldownComponent(0, null, 300)
        );
    }

    public Menu menu() {
        return menu;
    }
}
