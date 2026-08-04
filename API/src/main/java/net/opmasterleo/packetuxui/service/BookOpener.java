package net.opmasterleo.packetuxui.service;

import java.lang.reflect.Method;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.opmasterleo.packetuxui.common.StringUtils;
import net.opmasterleo.packetuxui.nms.MenuPacketBridge;

/**
 * Opens a written-book viewer without permanently mutating the player inventory when possible.
 */
final class BookOpener {

    private static final Method OPEN_BOOK_ITEMSTACK = findOpenBookItemStack();

    private BookOpener() {
    }

    static boolean open(Player player, BookView view, MenuPacketBridge packets) {
        if (player == null || view == null) {
            return false;
        }
        Book book = Book.book(view.title(), view.author(), view.pages());
        if (player instanceof Audience audience) {
            audience.openBook(book);
            return true;
        }
        if (packets != null && packets.openWrittenBook(player, view.title(), view.author(), view.pages())) {
            return true;
        }
        return openViaBukkitItem(player, view);
    }

    private static boolean openViaBukkitItem(Player player, BookView view) {
        if (OPEN_BOOK_ITEMSTACK == null) {
            return false;
        }
        ItemStack stack = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        meta.setTitle(plain(view.title()));
        meta.setAuthor(plain(view.author()));
        try {
            // Paper BookMeta pages(List<Component>)
            Method pagesComponents = meta.getClass().getMethod("pages", List.class);
            pagesComponents.invoke(meta, view.pages());
        } catch (ReflectiveOperationException ignored) {
            List<String> plainPages = new java.util.ArrayList<>(view.pages().size());
            for (Component page : view.pages()) {
                plainPages.add(plain(page));
            }
            meta.setPages(plainPages);
        }
        stack.setItemMeta(meta);
        try {
            OPEN_BOOK_ITEMSTACK.invoke(player, stack);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static String plain(Component component) {
        if (component == null) {
            return "";
        }
        String text = PlainTextComponentSerializer.plainText().serialize(component);
        if (!text.isEmpty()) {
            return text;
        }
        return StringUtils.toPlain(component);
    }

    private static Method findOpenBookItemStack() {
        try {
            return Player.class.getMethod("openBook", ItemStack.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
