package net.opmasterleo.packetuxui.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.common.StringUtils;
import net.opmasterleo.packetuxui.service.BookView;

/**
 * Fluent builder for a written-book text GUI ({@link BookView}).
 * <p>
 * Prefer {@link #newPage()} / {@link #page(Consumer)} so each line is its own call:
 * <pre>{@code
 * PacketMenus.book()
 *     .title("<gold>Rules")
 *     .author("Server")
 *     .newPage()
 *         .line("<gold>Welcome!")
 *         .blank()
 *         .line("<white>Be nice")
 *         .line("<gray>No cheating")
 *     .done()
 *     .newPage()
 *         .lines("<bold>Page 2", "", "<white>More text")
 *     .done()
 *     .open(player);
 * }</pre>
 */
public final class BookBuild {

    private Component title = Component.empty();
    private Component author = Component.empty();
    private final List<Component> pages = new ArrayList<>();
    private Consumer<Player> onClose;

    public static BookBuild create() {
        return new BookBuild();
    }

    public BookBuild title(Component title) {
        this.title = title == null ? Component.empty() : title;
        return this;
    }

    public BookBuild title(String miniMessageOrLegacy) {
        return title(StringUtils.toComponent(miniMessageOrLegacy == null ? "" : miniMessageOrLegacy));
    }

    public BookBuild author(Component author) {
        this.author = author == null ? Component.empty() : author;
        return this;
    }

    public BookBuild author(String miniMessageOrLegacy) {
        return author(StringUtils.toComponent(miniMessageOrLegacy == null ? "" : miniMessageOrLegacy));
    }

    /**
     * Start a multi-line page. Call {@link BookPageBuild#line(String)} / {@link BookPageBuild#blank()}
     * then {@link BookPageBuild#done()}.
     */
    public BookPageBuild newPage() {
        return new BookPageBuild(this);
    }

    /**
     * Configure one page via callback (auto-{@code done()}).
     */
    public BookBuild page(Consumer<BookPageBuild> configure) {
        Objects.requireNonNull(configure, "configure");
        BookPageBuild page = new BookPageBuild(this);
        configure.accept(page);
        return addBuiltPage(page.materialize());
    }

    /** Raw page component (single block — use {@link #newPage()} for stacked lines). */
    public BookBuild page(Component page) {
        pages.add(page == null ? Component.empty() : page);
        return this;
    }

    /** Single MiniMessage page string (may still contain {@code <newline>} if you want). */
    public BookBuild page(String miniMessageOrLegacy) {
        return page(StringUtils.toComponent(miniMessageOrLegacy == null ? "" : miniMessageOrLegacy));
    }

    /**
     * One page from stacked MiniMessage lines (empty strings → blank lines).
     * Same as {@code newPage().lines(...).done()}.
     */
    public BookBuild pageLines(String... miniMessageLines) {
        return newPage().lines(miniMessageLines).done();
    }

    public BookBuild pageLines(Component... lines) {
        return newPage().lines(lines).done();
    }

    public BookBuild pages(Collection<Component> pages) {
        Objects.requireNonNull(pages, "pages");
        for (Component page : pages) {
            page(page);
        }
        return this;
    }

    public BookBuild pagesMini(Collection<String> pages) {
        Objects.requireNonNull(pages, "pages");
        for (String page : pages) {
            page(page);
        }
        return this;
    }

    public BookBuild clearPages() {
        pages.clear();
        return this;
    }

    /**
     * Best-effort close hook — invoked when the book is displaced (inventory GUI, another book,
     * {@code close}, quit). Not guaranteed on Esc alone (client sends no container close).
     */
    public BookBuild onClose(Consumer<Player> onClose) {
        this.onClose = onClose;
        return this;
    }

    BookBuild addBuiltPage(Component page) {
        pages.add(page == null ? Component.empty() : page);
        return this;
    }

    public BookView build() {
        return new BookView(title, author, pages, onClose);
    }

    /** Materialize and open for {@code player}. */
    public void open(Player player) {
        PacketGuiManager.ofApi().openBook(player, build());
    }
}
