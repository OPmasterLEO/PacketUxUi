package net.opmasterleo.packetuxui;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.event.GuiEventManager;
import net.opmasterleo.packetuxui.event.GuiListener;
import net.opmasterleo.packetuxui.event.GuiOpenListener;
import net.opmasterleo.packetuxui.manager.BookBuild;
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

    public static GuiEventManager events() {
        return PacketUxUiAPI.getEventManager();
    }

    public static void registerListener(GuiListener listener) {
        events().register(listener);
    }

    public static void unregisterListener(GuiListener listener) {
        events().unregister(listener);
    }

    /**
     * Efficient InventoryOpenEvent analogue — direct call, no Bukkit bus.
     * Zero alloc/cost on open when nothing is registered.
     * Also fires on silent type-swaps ({@link net.opmasterleo.packetuxui.event.GuiOpenReason#TYPE_SWAP}).
     */
    public static void onInventoryOpen(GuiOpenListener listener) {
        events().registerOpen(listener);
    }

    public static void unregisterInventoryOpen(GuiOpenListener listener) {
        events().unregisterOpen(listener);
    }

    public static PacketGuiManager gui() {
        return PacketGuiManager.ofApi();
    }

    public static MenuBuild build() {
        return MenuBuild.create();
    }

    public static BookBuild book() {
        return BookBuild.create();
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

    public static void openBook(Player player, net.opmasterleo.packetuxui.service.BookView view) {
        gui().openBook(player, view);
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

    public static void setDebugLogging(boolean enabled) {
        service().setDebugLogging(enabled);
    }

    public static boolean debugLogging() {
        return service().debugLogging();
    }

    public static MenuMode readOnly() {
        return MenuMode.READ_ONLY;
    }

    public static MenuMode editable() {
        return MenuMode.EDITABLE;
    }
}
