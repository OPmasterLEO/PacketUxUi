package net.opmasterleo.packetuxui.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.service.MenuMode;
import net.opmasterleo.packetuxui.types.InventoryType;

class GuiOpenListenerTest {

    @Test
    void openOnlyHookFiresWithoutGuiListenerAllocPressure() {
        GuiEventManager manager = new GuiEventManager();
        AtomicInteger opens = new AtomicInteger();
        manager.registerOpen(event -> {
            opens.incrementAndGet();
            assertEquals(GuiOpenReason.TYPE_SWAP, event.reason());
            assertTrue(event.isTypeSwap());
        });
        // Click-only GuiListener must not force open interest.
        manager.register(new GuiListener() {
            @Override
            public void onClick(GuiClickEvent event) {
            }
        });
        assertTrue(manager.hasOpenListeners());

        Menu menu = new Menu(Component.text("t"), InventoryType.GENERIC9X1, Map.of(), null, MenuMode.READ_ONLY);
        manager.fireOpen(new GuiOpenEvent(null, menu, 1, 9, 0, GuiOpenReason.TYPE_SWAP));
        assertEquals(1, opens.get());
    }

    @Test
    void clickOnlyListenerDoesNotCountAsOpenInterest() {
        GuiEventManager manager = new GuiEventManager();
        manager.register(new GuiListener() {
            @Override
            public void onClick(GuiClickEvent event) {
            }
        });
        assertFalse(manager.hasOpenListeners());
        assertTrue(manager.hasListeners());
    }

    @Test
    void guiListenerOnOpenIsDetected() {
        GuiEventManager manager = new GuiEventManager();
        List<GuiOpenReason> reasons = new ArrayList<>();
        manager.register(new GuiListener() {
            @Override
            public void onOpen(GuiOpenEvent event) {
                reasons.add(event.reason());
            }
        });
        assertTrue(manager.hasOpenListeners());
        Menu menu = new Menu(Component.text("t"), InventoryType.HOPPER, Map.of(), null, MenuMode.READ_ONLY);
        manager.fireOpen(new GuiOpenEvent(null, menu, 2, 5, 1, GuiOpenReason.OPEN));
        assertEquals(List.of(GuiOpenReason.OPEN), reasons);
    }
}
