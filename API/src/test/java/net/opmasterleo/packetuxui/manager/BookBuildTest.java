package net.opmasterleo.packetuxui.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
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
}
