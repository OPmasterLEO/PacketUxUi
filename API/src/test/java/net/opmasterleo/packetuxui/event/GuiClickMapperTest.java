package net.opmasterleo.packetuxui.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.dto.CooldownComponent;
import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.WindowClickType;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.service.MenuMode;
import net.opmasterleo.packetuxui.types.ClickData;
import net.opmasterleo.packetuxui.types.InventoryType;

class GuiClickMapperTest {

    @Test
    void slotTypes() {
        assertEquals(GuiSlotType.CONTAINER, GuiClickMapper.slotType(5, 27));
        assertEquals(GuiSlotType.PLAYER, GuiClickMapper.slotType(30, 27));
        assertEquals(GuiSlotType.HOTBAR, GuiClickMapper.slotType(54, 27));
        assertEquals(GuiSlotType.OUTSIDE, GuiClickMapper.slotType(-999, 27));
    }

    @Test
    void pickupAndShiftActions() {
        Menu menu = new Menu(
                Component.text("t"),
                InventoryType.GENERIC9X3,
                Map.of(),
                new CooldownComponent(),
                MenuMode.READ_ONLY
        );
        UxItem stone = UxItem.builder("minecraft:stone").amount(16).build();
        ClickPacket leftPickup = new ClickPacket(1, 1, 0, 0, 0, WindowClickType.PICKUP, Map.of(), UxItem.EMPTY);
        assertEquals(
                GuiClickAction.PICKUP_ALL,
                GuiClickMapper.action(leftPickup, ClickData.LEFT_PICKUP, stone, UxItem.EMPTY)
        );
        ClickPacket rightPickup = new ClickPacket(1, 1, 0, 1, 0, WindowClickType.PICKUP, Map.of(), UxItem.EMPTY);
        assertEquals(
                GuiClickAction.PICKUP_HALF,
                GuiClickMapper.action(rightPickup, ClickData.RIGHT_PICKUP, stone, UxItem.EMPTY)
        );
        ClickPacket shift = new ClickPacket(1, 1, 0, 0, 0, WindowClickType.QUICK_MOVE, Map.of(), UxItem.EMPTY);
        assertEquals(
                GuiClickAction.MOVE_TO_OTHER_INVENTORY,
                GuiClickMapper.action(shift, ClickData.SHIFT_LEFT, stone, UxItem.EMPTY)
        );
        assertEquals(UxItem.EMPTY, GuiClickMapper.currentItem(menu, 0, 27));
    }
}
