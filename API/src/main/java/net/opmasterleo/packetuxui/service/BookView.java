package net.opmasterleo.packetuxui.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.opmasterleo.packetuxui.nms.LiveLimits;

/**
 * Immutable written-book viewer payload.
 * <p>
 * Page/character caps come from live NMS ({@link LiveLimits#bookMaxPages()},
 * {@link LiveLimits#bookMaxPageLength()}) — not hardcoded. Page turns are client-side.
 * There are no inventory slots — use Adventure {@code ClickEvent}s on page text for actions.
 * Closing the book does not send a container close; {@link #onClose()} runs when the book
 * is displaced (another GUI, {@code close}, quit) — not reliably on Esc alone.
 */
public final class BookView {

    /**
     * @deprecated use {@link #maxPages()} / {@link LiveLimits#bookMaxPages()} (live NMS)
     */
    @Deprecated
    public static final int MAX_PAGES = 100;

    private final Component title;
    private final Component author;
    private final List<Component> pages;
    private final Consumer<Player> onClose;

    public BookView(
            Component title,
            Component author,
            List<Component> pages,
            Consumer<Player> onClose
    ) {
        this.title = title == null ? Component.empty() : title;
        this.author = author == null ? Component.empty() : author;
        List<Component> raw = pages == null ? List.of() : pages;
        int maxPages = maxPages();
        if (raw.size() > maxPages) {
            throw new IllegalArgumentException(
                    "book pages exceed NMS limit (" + maxPages + ")"
            );
        }
        int maxLen = LiveLimits.bookMaxPageLength();
        List<Component> copy = new ArrayList<>(Math.max(1, raw.size()));
        if (raw.isEmpty()) {
            copy.add(Component.empty());
        } else {
            for (Component page : raw) {
                Component safe = page == null ? Component.empty() : page;
                enforcePageLength(safe, maxLen);
                copy.add(safe);
            }
        }
        this.pages = List.copyOf(copy);
        this.onClose = onClose;
    }

    /** Live NMS {@code WritableBookContent.MAX_PAGES}. */
    public static int maxPages() {
        return LiveLimits.bookMaxPages();
    }

    /** Live NMS {@code WritableBookContent.PAGE_EDIT_LENGTH}. */
    public static int maxPageLength() {
        return LiveLimits.bookMaxPageLength();
    }

    private static void enforcePageLength(Component page, int maxLen) {
        if (maxLen <= 0) {
            return;
        }
        String plain = PlainTextComponentSerializer.plainText().serialize(page);
        if (plain.length() > maxLen) {
            throw new IllegalArgumentException(
                    "book page exceeds NMS length limit (" + maxLen + " chars, got " + plain.length() + ")"
            );
        }
    }

    public Component title() {
        return title;
    }

    public Component author() {
        return author;
    }

    public List<Component> pages() {
        return pages;
    }

    public Consumer<Player> onClose() {
        return onClose;
    }

    public BookView withOnClose(Consumer<Player> onClose) {
        return new BookView(title, author, pages, onClose);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BookView other)) {
            return false;
        }
        return title.equals(other.title)
                && author.equals(other.author)
                && pages.equals(other.pages)
                && Objects.equals(onClose, other.onClose);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, author, pages, onClose);
    }
}
