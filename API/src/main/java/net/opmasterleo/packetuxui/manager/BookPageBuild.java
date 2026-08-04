package net.opmasterleo.packetuxui.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.opmasterleo.packetuxui.common.StringUtils;

/**
 * Builds one book page as stacked lines (no {@code \n} required).
 * <p>
 * Finish with {@link #done()} to return to {@link BookBuild}, or use
 * {@link BookBuild#page(java.util.function.Consumer)} which calls {@code done()} for you.
 */
public final class BookPageBuild {

    private final BookBuild parent;
    private final List<Component> lines = new ArrayList<>();

    BookPageBuild(BookBuild parent) {
        this.parent = Objects.requireNonNull(parent, "parent");
    }

    /** Append a MiniMessage / legacy line. */
    public BookPageBuild line(String miniMessageOrLegacy) {
        return line(StringUtils.toComponent(miniMessageOrLegacy == null ? "" : miniMessageOrLegacy));
    }

    /** Append a component line. */
    public BookPageBuild line(Component line) {
        lines.add(line == null ? Component.empty() : line);
        return this;
    }

    /** Empty line (visual gap). */
    public BookPageBuild blank() {
        return line(Component.empty());
    }

    /** {@code count} empty lines. */
    public BookPageBuild blank(int count) {
        int n = Math.max(0, count);
        for (int i = 0; i < n; i++) {
            blank();
        }
        return this;
    }

    /** Several MiniMessage lines in order. Empty / null entries become blank lines. */
    public BookPageBuild lines(String... miniMessageLines) {
        if (miniMessageLines == null) {
            return this;
        }
        for (String line : miniMessageLines) {
            if (line == null || line.isEmpty()) {
                blank();
            } else {
                line(line);
            }
        }
        return this;
    }

    /** Several component lines in order. */
    public BookPageBuild lines(Component... componentLines) {
        if (componentLines == null) {
            return this;
        }
        for (Component line : componentLines) {
            line(line);
        }
        return this;
    }

    public BookPageBuild lines(Collection<String> miniMessageLines) {
        Objects.requireNonNull(miniMessageLines, "miniMessageLines");
        for (String line : miniMessageLines) {
            if (line == null || line.isEmpty()) {
                blank();
            } else {
                line(line);
            }
        }
        return this;
    }

    /**
     * Line then a blank (paragraph spacing).
     */
    public BookPageBuild paragraph(String miniMessageOrLegacy) {
        line(miniMessageOrLegacy);
        return blank();
    }

    public BookPageBuild paragraph(Component text) {
        line(text);
        return blank();
    }

    /** Commit this page and continue configuring the book. */
    public BookBuild done() {
        return parent.addBuiltPage(materialize());
    }

    Component materialize() {
        if (lines.isEmpty()) {
            return Component.empty();
        }
        if (lines.size() == 1) {
            return lines.get(0);
        }
        TextComponent.Builder builder = Component.text();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                builder.append(Component.newline());
            }
            builder.append(lines.get(i));
        }
        return builder.build();
    }
}
