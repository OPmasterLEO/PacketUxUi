package net.opmasterleo.packetuxui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.WindowClickType;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.types.ClickData;
import net.opmasterleo.packetuxui.types.ClickType;

/**
 * getClickType uses post-click carried semantics (same for left and right).
 */
class ClickTypeMappingTest {

    @Test
    void rightClickPostCarriedMatchesLeftSemantics() {
        MenuService service = new MenuService(null, null);
        ClickPacket rightPickup = click(1, false);
        ClickPacket rightPlace = click(1, true);
        assertEquals(ClickType.PICKUP, service.getClickType(rightPickup).clickType());
        assertEquals(ClickData.RIGHT_PICKUP.buttonType(), service.getClickType(rightPickup).buttonType());
        assertEquals(ClickType.PLACE, service.getClickType(rightPlace).clickType());
        assertEquals(ClickData.RIGHT_PLACE.buttonType(), service.getClickType(rightPlace).buttonType());
    }

    @Test
    void leftClickPostCarried() {
        MenuService service = new MenuService(null, null);
        assertEquals(ClickType.PICKUP, service.getClickType(click(0, false)).clickType());
        assertEquals(ClickType.PLACE, service.getClickType(click(0, true)).clickType());
    }

    private static ClickPacket click(int button, boolean carriedEmpty) {
        UxItem carried = carriedEmpty
                ? UxItem.EMPTY
                : UxItem.builder("minecraft:stone").amount(1).build();
        return new ClickPacket(1, 1, 0, button, 0, WindowClickType.PICKUP, Map.of(), carried);
    }
}
