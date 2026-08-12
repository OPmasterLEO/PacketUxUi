package net.opmasterleo.packetuxui.test.menus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
import net.opmasterleo.packetuxui.types.ExecuteComponent;
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
                .click(GlowingClick.INSTANCE)
                .build();

        Button cooldownButton = new ButtonBuilder()
                .item(redWool)
                .click(RedWoolClick.INSTANCE)
                .cooldown(new CooldownComponent(4000, ButtonCooldownMessage.INSTANCE, 1000))
                .build();

        Button staticButton = new ButtonBuilder()
                .item(coolSign)
                .click(CoolSignClick.INSTANCE)
                .build();

        Map<Integer, Button> buttons = new HashMap<>();
        for (int slot = 0; slot < 36; slot++) {
            if (UPDATE_BUTTONS.contains(slot)) {
                buttons.put(slot, new ButtonBuilder()
                        .item(stone)
                        .click(new ClearSlotClick(service, air, slot))
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
                new CooldownComponent(6000, MenuCooldownMessage.INSTANCE, 1200)
        );

        scheduler.runRepeatingGlobal(new ShowcaseTicker(service, scheduler, menu, stone, air), 4L);
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

    private static final class GlowingClick implements ExecuteComponent.Handler {
        private static final GlowingClick INSTANCE = new GlowingClick();

        @Override
        public void accept(ExecuteComponent it) {
            it.player().sendMessage(StringUtils.toComponent("<green>You clicked on the glowing button!"));
            it.player().sendMessage(StringUtils.toComponent("Button type: " + it.buttonType()));
        }
    }

    private static final class RedWoolClick implements ExecuteComponent.Handler {
        private static final RedWoolClick INSTANCE = new RedWoolClick();

        @Override
        public void accept(ExecuteComponent it) {
            it.player().sendMessage(StringUtils.toComponent("<gold>Clicked on Red Wool!"));
            String key = it.itemStack() == null ? "null" : it.itemStack().materialKey();
            it.player().sendMessage(StringUtils.toComponent("Item type: " + key));
        }
    }

    private static final class CoolSignClick implements ExecuteComponent.Handler {
        private static final CoolSignClick INSTANCE = new CoolSignClick();

        @Override
        public void accept(ExecuteComponent it) {
            Player player = it.player();
            net.opmasterleo.packetuxui.PacketMenus.closeThen(player, () ->
                    net.opmasterleo.packetuxui.PacketMenus.sign()
                            .line(0, "")
                            .line(1, "<gray>^^^^^^^^")
                            .line(2, "<gold>Type above")
                            .line(3, "<dark_gray>Sign input")
                            .onFinish((p, result) -> {
                                String text = result.plain(0);
                                if (text.isEmpty()) {
                                    p.sendMessage(StringUtils.toComponent("<red>Type something on the first line."));
                                    return net.opmasterleo.packetuxui.service.SignAction.reopen(
                                            "",
                                            "<gray>^^^^^^^^",
                                            "<gold>Type above",
                                            "<dark_gray>Sign input"
                                    );
                                }
                                p.sendMessage(StringUtils.toComponent("<green>You wrote: <white>" + text));
                                return net.opmasterleo.packetuxui.service.SignAction.close();
                            })
                            .open(player)
            );
        }
    }

    private static final class ButtonCooldownMessage implements ExecuteComponent.Handler {
        private static final ButtonCooldownMessage INSTANCE = new ButtonCooldownMessage();

        @Override
        public void accept(ExecuteComponent it) {
            it.player().sendMessage(StringUtils.toComponent("<red>Cooldown active. Wait before clicking again."));
        }
    }

    private static final class MenuCooldownMessage implements ExecuteComponent.Handler {
        private static final MenuCooldownMessage INSTANCE = new MenuCooldownMessage();

        @Override
        public void accept(ExecuteComponent it) {
            it.player().sendMessage(StringUtils.toComponent("<yellow>Menu is on cooldown!"));
        }
    }

    private static final class ClearSlotClick implements ExecuteComponent.Handler {
        private final MenuService service;
        private final UxItem air;
        private final int slot;

        private ClearSlotClick(MenuService service, UxItem air, int slot) {
            this.service = service;
            this.air = air;
            this.slot = slot;
        }

        @Override
        public void accept(ExecuteComponent it) {
            service.updateItem(it.player(), air, slot);
        }
    }

    private static final class ShowcaseTicker implements Runnable {
        private final MenuService service;
        private final PlatformScheduler scheduler;
        private final Menu menu;
        private final UxItem stone;
        private final UxItem air;

        private ShowcaseTicker(
                MenuService service,
                PlatformScheduler scheduler,
                Menu menu,
                UxItem stone,
                UxItem air
        ) {
            this.service = service;
            this.scheduler = scheduler;
            this.menu = menu;
            this.stone = stone;
            this.air = air;
        }

        @Override
        public void run() {
            for (Player player : Bukkit.getOnlinePlayers()) {
                scheduler.runForPlayer(player, new PlayerTick(service, menu, stone, air, player));
            }
        }
    }

    private static final class PlayerTick implements Runnable {
        private final MenuService service;
        private final Menu menu;
        private final UxItem stone;
        private final UxItem air;
        private final Player player;

        private PlayerTick(MenuService service, Menu menu, UxItem stone, UxItem air, Player player) {
            this.service = service;
            this.menu = menu;
            this.stone = stone;
            this.air = air;
            this.player = player;
        }

        @Override
        public void run() {
            Menu open = service.getMenu(player);
            if (open == null || !open.name().equals(menu.name())) {
                return;
            }
            for (int slot : UPDATE_BUTTONS) {
                if (chance(20)) {
                    service.updateItem(player, chance(50) ? stone : air, slot);
                }
            }
        }
    }
}
