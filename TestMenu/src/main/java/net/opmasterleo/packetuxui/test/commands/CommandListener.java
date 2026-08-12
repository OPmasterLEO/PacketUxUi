package net.opmasterleo.packetuxui.test.commands;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.context.CommandContext;
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
                            .handler(new OpenMenuHandler(service, menu))
            );
            commandManager.command(
                    openMenuBuilder.literal(subcommand)
                            .literal("desc")
                            .handler(new DescHandler(description))
            );
        }

        commandManager.command(
                openMenuBuilder.literal("sign")
                        .handler(new OpenSignHandler())
        );
        commandManager.command(
                openMenuBuilder.literal("sign")
                        .literal("desc")
                        .handler(new DescHandler("Packet sign editor input."))
        );
    }

    private record MenuEntry(Menu menu, String description) {
    }

    private static final class OpenMenuHandler implements org.incendo.cloud.execution.CommandExecutionHandler<PlayerSource> {
        private final MenuService service;
        private final Menu menu;

        private OpenMenuHandler(MenuService service, Menu menu) {
            this.service = service;
            this.menu = menu;
        }

        @Override
        public void execute(CommandContext<PlayerSource> context) {
            service.openMenu(context.sender().source(), menu);
        }
    }

    private static final class DescHandler implements org.incendo.cloud.execution.CommandExecutionHandler<PlayerSource> {
        private final String description;

        private DescHandler(String description) {
            this.description = description;
        }

        @Override
        public void execute(CommandContext<PlayerSource> context) {
            context.sender().source().sendMessage(description);
        }
    }

    private static final class OpenSignHandler implements org.incendo.cloud.execution.CommandExecutionHandler<PlayerSource> {
        @Override
        public void execute(CommandContext<PlayerSource> context) {
            org.bukkit.entity.Player player = context.sender().source();
            net.opmasterleo.packetuxui.PacketMenus.sign()
                    .line(0, "")
                    .line(1, "<gray>^^^^^^^^")
                    .line(2, "<gold>Type above")
                    .onFinish((p, result) -> {
                        String text = result.plain(0);
                        if (text.isEmpty()) {
                            p.sendMessage("Type something on the first line.");
                            return net.opmasterleo.packetuxui.service.SignAction.reopen(
                                    "",
                                    "<gray>^^^^^^^^",
                                    "<gold>Type above"
                            );
                        }
                        p.sendMessage("You wrote: " + text);
                        return net.opmasterleo.packetuxui.service.SignAction.close();
                    })
                    .open(player);
        }
    }
}
