package net.opmasterleo.packetuxui.test.commands;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper;
import org.incendo.cloud.paper.util.sender.PlayerSource;
import org.incendo.cloud.paper.util.sender.Source;

import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.service.MenuService;
import net.opmasterleo.packetuxui.test.menus.AllInOne;
import net.opmasterleo.packetuxui.test.menus.ButtonDetector;
import net.opmasterleo.packetuxui.test.menus.CooldownTest;
import net.opmasterleo.packetuxui.test.menus.Dynamic4x9;
import net.opmasterleo.packetuxui.test.menus.Static3x9;

public final class CommandListener {

    public CommandListener(JavaPlugin plugin, MenuService service) {
        Static3x9 static3x9 = new Static3x9();
        Dynamic4x9 dynamic4x9 = new Dynamic4x9(service);
        ButtonDetector buttonDetector = new ButtonDetector();
        CooldownTest cooldownTest = new CooldownTest();
        AllInOne allInOne = new AllInOne(service);

        PaperCommandManager<Source> commandManager = PaperCommandManager
                .builder(PaperSimpleSenderMapper.simpleSenderMapper())
                .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
                .buildOnEnable(plugin);

        var openMenuBuilder = commandManager.commandBuilder("open_menu")
                .senderType(PlayerSource.class);

        Map<String, MenuEntry> subcommands = new LinkedHashMap<>();
        subcommands.put("static_3x9", new MenuEntry(static3x9.menu(), "Simple static menu 3x9."));
        subcommands.put("dynamic_4x9", new MenuEntry(dynamic4x9.menu(), "Dynamic menu 4x9."));
        subcommands.put("button_detector", new MenuEntry(buttonDetector.menu(), "Static crafting table menu."));
        subcommands.put("cooldown_test", new MenuEntry(cooldownTest.menu(), "Static anvil menu."));
        subcommands.put("all_in_one", new MenuEntry(allInOne.menu(), "Feature showcase menu."));

        for (Map.Entry<String, MenuEntry> entry : subcommands.entrySet()) {
            String subcommand = entry.getKey();
            Menu menu = entry.getValue().menu();
            String description = entry.getValue().description();
            commandManager.command(
                    openMenuBuilder.literal(subcommand)
                            .handler(context -> service.openMenu(context.sender().source(), menu))
            );
            commandManager.command(
                    openMenuBuilder.literal(subcommand)
                            .literal("desc")
                            .handler(context -> context.sender().source().sendMessage(description))
            );
        }
    }

    private record MenuEntry(Menu menu, String description) {
    }
}
