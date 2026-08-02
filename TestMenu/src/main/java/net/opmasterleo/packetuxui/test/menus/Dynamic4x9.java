package net.opmasterleo.packetuxui.test.menus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;

import net.opmasterleo.packetuxui.PacketUxUiAPI;
import net.opmasterleo.packetuxui.common.StringUtils;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.nms.item.UxItemBuilder;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.service.Button;
import net.opmasterleo.packetuxui.service.ButtonBuilder;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.service.MenuService;
import net.opmasterleo.packetuxui.types.InventoryType;

public final class Dynamic4x9 {

    private final Menu menu;

    public Dynamic4x9(MenuService service) {
        UxItem stone = new UxItemBuilder().material("minecraft:stone").build();
        UxItem air = UxItem.EMPTY;
        PlatformScheduler scheduler = PacketUxUiAPI.getScheduler();

        Map<Integer, Button> buttons = new HashMap<>();
        for (int slot = 0; slot < 27; slot++) {
            int captured = slot;
            buttons.put(slot, new ButtonBuilder()
                    .item(new UxItemBuilder()
                            .material(slot % 2 == 0
                                    ? "minecraft:blue_stained_glass_pane"
                                    : "minecraft:pink_stained_glass_pane")
                            .name(StringUtils.toComponent("<dark_gray><italic>Background Tile"))
                            .build())
                    .click(it -> service.updateItem(it.player(), stone, captured))
                    .build());
        }

        this.menu = new Menu(
                StringUtils.toComponent("<gradient:#ff1493:#1e90ff><bold>Styled Background"),
                InventoryType.GENERIC9X3,
                buttons
        );

        scheduler.runRepeatingGlobal(() -> {
            for (var player : Bukkit.getOnlinePlayers()) {
                scheduler.runForPlayer(player, () -> {
                    Menu open = service.getMenu(player);
                    if (open == null || !open.name().equals(menu.name())) {
                        return;
                    }
                    for (int i = 0; i < 27; i++) {
                        if (chance(10)) {
                            if (chance(50)) {
                                Button button = menu.buttons().get(i);
                                if (button != null) {
                                    service.updateItem(player, button.item(), i);
                                }
                            } else {
                                service.updateItem(player, air, i);
                            }
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
