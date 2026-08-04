package net.opmasterleo.packetuxui.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.WindowClickType;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.service.MenuMode;
import net.opmasterleo.packetuxui.types.ClickData;
import net.opmasterleo.packetuxui.types.InventoryType;

class GuiEventManagerTest {

    @Test
    void clickCancelAndPriorityOrder() {
        GuiEventManager manager = new GuiEventManager();
        List<String> order = new ArrayList<>();
        AtomicBoolean cancelledSeen = new AtomicBoolean();

        manager.register(new GuiListener() {
            @Override
            public GuiListenerPriority priority() {
                return GuiListenerPriority.LOW;
            }

            @Override
            public void onClick(GuiClickEvent event) {
                order.add("low");
                event.setCancelled(true);
            }
        });
        manager.register(new GuiListener() {
            @Override
            public GuiListenerPriority priority() {
                return GuiListenerPriority.HIGH;
            }

            @Override
            public void onClick(GuiClickEvent event) {
                order.add("high");
                cancelledSeen.set(event.isCancelled());
            }
        });

        Menu menu = new Menu(
                Component.text("t"),
                InventoryType.GENERIC9X1,
                Map.of(),
                new net.opmasterleo.packetuxui.dto.CooldownComponent(),
                MenuMode.READ_ONLY
        );
        ClickPacket packet = new ClickPacket(1, 1, 0, 0, 0, WindowClickType.PICKUP, Map.of(), UxItem.EMPTY);
        GuiClickEvent event = new GuiClickEvent(
                null, menu, 1, 9, 1, packet, ClickData.LEFT_PICKUP, UxItem.EMPTY
        );
        manager.fireClick(event);

        assertEquals(List.of("low", "high"), order);
        assertTrue(event.isCancelled());
        assertTrue(cancelledSeen.get());
        assertEquals(GuiClickAction.NOTHING, event.action()); // empty slot pickup
        assertEquals(GuiSlotType.CONTAINER, event.slotType());
    }
}
