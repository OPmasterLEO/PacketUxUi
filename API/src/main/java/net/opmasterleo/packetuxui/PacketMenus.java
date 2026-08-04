package net.opmasterleo.packetuxui;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.manager.MenuBuild;
import net.opmasterleo.packetuxui.manager.PacketGuiManager;
import net.opmasterleo.packetuxui.nms.item.UxHeadItemBuilder;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.nms.item.UxItemBuilder;
import net.opmasterleo.packetuxui.service.Button;
import net.opmasterleo.packetuxui.service.ButtonBuilder;
import net.opmasterleo.packetuxui.service.GuiScopeListener;
import net.opmasterleo.packetuxui.service.IButtonBuilder;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.service.MenuBuilder;
import net.opmasterleo.packetuxui.service.MenuMode;
import net.opmasterleo.packetuxui.service.MenuService;
import net.opmasterleo.packetuxui.types.InventoryType;

public final class PacketMenus {

    private PacketMenus() {
    }

    public static MenuService service() {
        return PacketUxUiAPI.getService();
    }

    public static PacketGuiManager gui() {
        return PacketGuiManager.ofApi();
    }

    public static MenuBuild build() {
        return MenuBuild.create();
    }

    public static MenuBuilder menu(Component title, InventoryType type) {
        return PacketUxUiAPI.menu(title, type);
    }

    public static MenuBuilder menu(String miniMessageTitle, InventoryType type) {
        return PacketUxUiAPI.menu(miniMessageTitle, type);
    }

    public static IButtonBuilder button() {
        return new ButtonBuilder();
    }

    public static UxItemBuilder item() {
        return new UxItemBuilder();
    }

    public static UxItemBuilder item(String materialKey) {
        return new UxItemBuilder().material(materialKey);
    }

    public static UxHeadItemBuilder skull() {
        return new UxHeadItemBuilder();
    }

    public static void open(Player player, Menu menu) {
        PacketUxUiAPI.open(player, menu);
    }

    public static void close(Player player) {
        PacketUxUiAPI.close(player);
    }

    public static void closeThen(Player player, Runnable onSettled) {
        gui().closeThen(player, onSettled);
    }

    public static void closeThen(Player player, long settleTicks, Runnable onSettled) {
        gui().closeThen(player, settleTicks, onSettled);
    }

    public static void present(Player player, Menu menu) {
        service().present(player, menu);
    }

    public static void reopen(Player player, Menu menu) {
        gui().reopen(player, menu);
    }

    public static void updateItem(Player player, int slot, UxItem item) {
        service().updateItem(player, item, slot);
    }

    public static void patchSlot(Player player, int slot, UxItem item) {
        service().updateItem(player, item, slot);
    }

    public static void updateButton(Player player, int slot, Button button) {
        service().updateButton(player, button, slot);
    }

    public static void refresh(Player player) {
        service().refreshWindow(player);
    }

    public static int getWindowId(Player player) {
        return service().getWindowId(player);
    }

    public static int getTopSlotCount(Player player) {
        return service().getTopSlotCount(player);
    }

    public static void setScopeListener(GuiScopeListener listener) {
        gui().setScopeListener(listener);
    }

    public static MenuMode readOnly() {
        return MenuMode.READ_ONLY;
    }

    public static MenuMode editable() {
        return MenuMode.EDITABLE;
    }
}
