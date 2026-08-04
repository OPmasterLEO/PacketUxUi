package net.opmasterleo.packetuxui.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

/**
 * Immutable written-book viewer payload.
 * <p>
 * <b>Limits (vanilla client):</b> at most {@link #MAX_PAGES} pages; each page is a single
 * text component (keep under ~1023 characters / ~14 lines). Page turns are client-side.
 * There are no inventory slots — use Adventure {@code ClickEvent}s on page text for actions
 * ({@code run_command}, {@code suggest_command}, {@code change_page}, {@code open_url}, …).
 * Closing the book does not send a container close; {@link #onClose()} runs when the book
 * is displaced (another GUI, {@code close}, quit) — not reliably on Esc alone.
 */
public final class BookView {

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
        if (raw.size() > MAX_PAGES) {
            throw new IllegalArgumentException("book pages exceed " + MAX_PAGES);
        }
        List<Component> copy = new ArrayList<>(Math.max(1, raw.size()));
        if (raw.isEmpty()) {
            copy.add(Component.empty());
        } else {
            for (Component page : raw) {
                copy.add(page == null ? Component.empty() : page);
            }
        }
        this.pages = List.copyOf(copy);
        this.onClose = onClose;
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
