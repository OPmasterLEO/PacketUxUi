package net.opmasterleo.packetuxui.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.WindowClickType;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.service.MenuMode;
import net.opmasterleo.packetuxui.types.ClickData;
import net.opmasterleo.packetuxui.types.InventoryType;

class GuiClickDragListenerTest {

    @Test
    void openOnlyDoesNotForceClickInterest() {
        GuiEventManager manager = new GuiEventManager();
        manager.registerOpen(event -> {
        });
        assertTrue(manager.hasOpenListeners());
        assertFalse(manager.hasClickListeners());
        assertFalse(manager.hasDragListeners());
    }

    @Test
    void dedicatedClickHookCancels() {
        GuiEventManager manager = new GuiEventManager();
        AtomicInteger clicks = new AtomicInteger();
        manager.registerClick(event -> {
            clicks.incrementAndGet();
            event.setCancelled(true);
        });
        assertTrue(manager.hasClickListeners());

        Menu menu = new Menu(Component.text("t"), InventoryType.GENERIC9X1, Map.of(), null, MenuMode.READ_ONLY);
        ClickPacket packet = new ClickPacket(1, 1, 0, 0, 0, WindowClickType.PICKUP, Map.of(), UxItem.EMPTY);
        GuiClickEvent event = new GuiClickEvent(
                null, menu, 1, 9, 1, packet, ClickData.LEFT_PICKUP, UxItem.EMPTY
        );
        manager.fireClick(event);
        assertEquals(1, clicks.get());
        assertTrue(event.isCancelled());
    }

    @Test
    void dedicatedDragHookFires() {
        GuiEventManager manager = new GuiEventManager();
        AtomicBoolean seen = new AtomicBoolean();
        manager.registerDrag(event -> {
            seen.set(true);
            assertEquals(GuiDragPhase.END, event.phase());
        });
        Menu menu = new Menu(Component.text("t"), InventoryType.GENERIC9X3, Map.of(), null, MenuMode.EDITABLE);
        ClickPacket packet = new ClickPacket(1, 1, 0, 0, 0, WindowClickType.QUICK_CRAFT, Map.of(), UxItem.EMPTY);
        GuiDragEvent event = new GuiDragEvent(
                null, menu, 1, 27, 1, packet, ClickData.LEFT_PICKUP, GuiDragPhase.END, java.util.Set.of(0, 1), UxItem.EMPTY
        );
        manager.fireDrag(event);
        assertTrue(seen.get());
    }
}
