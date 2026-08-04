package net.opmasterleo.packetuxui.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.opmasterleo.packetuxui.service.BookView;

class BookBuildTest {

    @Test
    void emptyPagesBecomesOneBlankPage() {
        BookView view = BookBuild.create().title("T").author("A").build();
        assertEquals(1, view.pages().size());
        assertEquals(Component.empty(), view.pages().get(0));
    }

    @Test
    void pagesAccumulate() {
        BookView view = BookBuild.create()
                .page("one")
                .page(Component.text("two"))
                .build();
        assertEquals(2, view.pages().size());
    }

    @Test
    void newPageStacksLinesWithNewlines() {
        BookView view = BookBuild.create()
                .newPage()
                    .line("Hello")
                    .blank()
                    .line("World")
                .done()
                .build();
        assertEquals(1, view.pages().size());
        String plain = PlainTextComponentSerializer.plainText().serialize(view.pages().get(0));
        assertEquals("Hello\n\nWorld", plain);
    }

    @Test
    void pageConsumerAndPageLines() {
        BookView view = BookBuild.create()
                .page(p -> p.line("A").line("B"))
                .pageLines("C", "", "D")
                .build();
        assertEquals(2, view.pages().size());
        assertEquals("A\nB", plain(view.pages().get(0)));
        assertEquals("C\n\nD", plain(view.pages().get(1)));
    }

    @Test
    void rejectsTooManyPages() {
        List<Component> pages = new ArrayList<>();
        for (int i = 0; i < BookView.MAX_PAGES + 1; i++) {
            pages.add(Component.text(String.valueOf(i)));
        }
        assertThrows(IllegalArgumentException.class, () -> new BookView(
                Component.empty(),
                Component.empty(),
                pages,
                null
        ));
    }

    @Test
    void maxPagesAllowed() {
        List<Component> pages = new ArrayList<>();
        for (int i = 0; i < BookView.MAX_PAGES; i++) {
            pages.add(Component.text(String.valueOf(i)));
        }
        BookView view = new BookView(Component.text("t"), Component.text("a"), pages, null);
        assertEquals(BookView.MAX_PAGES, view.pages().size());
        assertTrue(view.onClose() == null);
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
