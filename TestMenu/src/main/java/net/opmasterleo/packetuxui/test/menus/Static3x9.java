package net.opmasterleo.packetuxui.test.menus;

import java.util.Map;

import net.opmasterleo.packetuxui.common.StringUtils;
import net.opmasterleo.packetuxui.dto.CooldownComponent;
import net.opmasterleo.packetuxui.nms.item.UxItemBuilder;
import net.opmasterleo.packetuxui.service.Button;
import net.opmasterleo.packetuxui.service.ButtonBuilder;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.types.InventoryType;

public final class Static3x9 {

    private final Menu menu;

    public Static3x9() {
        var item1 = new UxItemBuilder()
                .material("minecraft:red_wool")
                .name(StringUtils.toComponent("<red>Red Wool"))
                .lore(
                        StringUtils.toComponent("<#f7983e>Red Wool"),
                        StringUtils.toComponent("<#f7b33e>Red Wool"),
                        StringUtils.toComponent("<#f7c93e>Red Wool")
                )
                .amount(4)
                .enchantment("minecraft:fire_aspect", 2, true)
                .build();

        var item2 = new UxItemBuilder()
                .material("minecraft:acacia_sign")
                .name(StringUtils.toComponent("<rainbow>Cool sign"))
                .lore(
                        StringUtils.toComponent("<#f7983e>smthn"),
                        StringUtils.toComponent("<#f7b33e>invisible enchantment")
                )
                .amount(64)
                .enchantment("minecraft:unbreaking", 2, false)
                .build();

        Button button1 = new ButtonBuilder()
                .item(item1)
                .click(it -> {
                    String key = it.itemStack() == null ? "null" : it.itemStack().materialKey();
                    it.player().sendMessage(StringUtils.toComponent("You clicked with item: " + key));
                })
                .build();

        Button button2 = new ButtonBuilder()
                .item(item2)
                .build();

        this.menu = new Menu(
                StringUtils.toComponent("<gradient:#ff6d2e:#ff2e62><bold>First packet menu"),
                InventoryType.GENERIC9X3,
                Map.of(
                        0, button1,
                        4, button1,
                        8, button2,
                        12, button2,
                        16, button1,
                        20, button1,
                        24, button2
                ),
                new CooldownComponent(
                        5000,
                        it -> it.player().sendMessage(
                                StringUtils.toComponent("Menu cooldown bigger so it overrides item's one")
                        ),
                        1000
                )
        );
    }

    public Menu menu() {
        return menu;
    }
}
