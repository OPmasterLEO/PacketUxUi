package net.opmasterleo.packetuxui.test.menus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;

import net.opmasterleo.packetuxui.PacketUxUiAPI;
import net.opmasterleo.packetuxui.common.StringUtils;
import net.opmasterleo.packetuxui.dto.CooldownComponent;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.nms.item.UxItemBuilder;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.service.Button;
import net.opmasterleo.packetuxui.service.ButtonBuilder;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.service.MenuService;
import net.opmasterleo.packetuxui.types.InventoryType;

public final class AllInOne {

    private static final List<Integer> UPDATE_BUTTONS = List.of(2, 4, 6, 8, 10);

    private final Menu menu;

    public AllInOne(MenuService service) {
        UxItem stone = new UxItemBuilder().material("minecraft:stone").build();
        UxItem air = UxItem.EMPTY;
        PlatformScheduler scheduler = PacketUxUiAPI.getScheduler();

        var glowingItem = new UxItemBuilder()
                .material("minecraft:glowstone")
                .name(StringUtils.toComponent("<yellow><bold>Glowing Stone"))
                .amount(1)
                .build();

        var redWool = new UxItemBuilder()
                .material("minecraft:red_wool")
                .name(StringUtils.toComponent("<red><bold>Red Wool"))
                .lore(
                        StringUtils.toComponent("<#f7983e>Shiny Red Wool"),
                        StringUtils.toComponent("<#f7b33e>Perfect for decoration.")
                )
                .amount(4)
                .enchantment("minecraft:fire_aspect", 2, true)
                .build();

        var coolSign = new UxItemBuilder()
                .material("minecraft:acacia_sign")
                .name(StringUtils.toComponent("<rainbow>Cool Sign"))
                .lore(StringUtils.toComponent("<gray>Invisible enchantment here."))
                .amount(64)
                .enchantment("minecraft:unbreaking", 2, false)
                .build();

        Button hoverButton = new ButtonBuilder()
                .item(glowingItem)
                .click(it -> {
                    it.player().sendMessage(StringUtils.toComponent("<green>You clicked on the glowing button!"));
                    it.player().sendMessage(StringUtils.toComponent("Button type: " + it.buttonType()));
                })
                .build();

        Button cooldownButton = new ButtonBuilder()
                .item(redWool)
                .click(it -> {
                    it.player().sendMessage(StringUtils.toComponent("<gold>Clicked on Red Wool!"));
                    String key = it.itemStack() == null ? "null" : it.itemStack().materialKey();
                    it.player().sendMessage(StringUtils.toComponent("Item type: " + key));
                })
                .cooldown(new CooldownComponent(
                        4000,
                        it -> it.player().sendMessage(
                                StringUtils.toComponent("<red>Cooldown active. Wait before clicking again.")
                        ),
                        1000
                ))
                .build();

        Button staticButton = new ButtonBuilder()
                .item(coolSign)
                .click(it -> it.player().sendMessage(
                        StringUtils.toComponent("<aqua>You clicked on the Cool Sign!")
                ))
                .build();

        Map<Integer, Button> buttons = new HashMap<>();
        for (int slot = 0; slot < 36; slot++) {
            int captured = slot;
            if (UPDATE_BUTTONS.contains(slot)) {
                buttons.put(slot, new ButtonBuilder()
                        .item(stone)
                        .click(it -> service.updateItem(it.player(), air, captured))
                        .build());
            } else if (slot % 9 == 0) {
                buttons.put(slot, hoverButton);
            } else if (slot % 9 == 4) {
                buttons.put(slot, cooldownButton);
            } else if (slot % 9 == 8) {
                buttons.put(slot, staticButton);
            } else {
                buttons.put(slot, new ButtonBuilder()
                        .item(new UxItemBuilder()
                                .material(slot % 2 == 0
                                        ? "minecraft:blue_stained_glass_pane"
                                        : "minecraft:pink_stained_glass_pane")
                                .name(StringUtils.toComponent("<gray><italic>Decorative Tile"))
                                .build())
                        .build());
            }
        }

        this.menu = new Menu(
                StringUtils.toComponent("<gradient:#ff6d2e:#1e90ff><bold>Feature Showcase Menu"),
                InventoryType.GENERIC9X4,
                buttons,
                new CooldownComponent(
                        6000,
                        it -> it.player().sendMessage(StringUtils.toComponent("<yellow>Menu is on cooldown!")),
                        1200
                )
        );

        scheduler.runRepeatingGlobal(() -> {
            for (var player : Bukkit.getOnlinePlayers()) {
                scheduler.runForPlayer(player, () -> {
                    Menu open = service.getMenu(player);
                    if (open == null || !open.name().equals(menu.name())) {
                        return;
                    }
                    for (int slot : UPDATE_BUTTONS) {
                        if (chance(20)) {
                            service.updateItem(player, chance(50) ? stone : air, slot);
                        }
                    }
                });
            }
        }, 4L);
    }

    public Menu menu() {
        return menu;
    }

    private static boolean chance(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("percent out of range");
        }
        return ThreadLocalRandom.current().nextFloat() * 100 < percent;
    }
}
