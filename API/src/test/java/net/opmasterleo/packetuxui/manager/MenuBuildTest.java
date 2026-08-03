package net.opmasterleo.packetuxui.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import net.opmasterleo.packetuxui.service.GuiScopeListener;
import net.opmasterleo.packetuxui.service.MenuMode;
import net.opmasterleo.packetuxui.types.InventoryType;

class MenuBuildTest {

    @Test
    void rowsMapToInventoryType() {
        assertEquals(InventoryType.GENERIC9X1, MenuBuild.create().rows(1).type());
        assertEquals(InventoryType.GENERIC9X6, MenuBuild.create().rows(6).type());
        assertEquals(MenuMode.EDITABLE, MenuBuild.create().editable().mode());
        assertEquals(MenuMode.READ_ONLY, MenuBuild.create().readOnly().mode());
    }

    @Test
    void scopeListenerOrderingContract() {
        List<String> events = new ArrayList<>();
        AtomicInteger top = new AtomicInteger();
        GuiScopeListener listener = (player, open, topSlotCount) -> {
            events.add(open ? "open:" + topSlotCount : "close:" + topSlotCount);
            top.set(topSlotCount);
        };
        listener.onScopeChanged(null, true, 27);
        listener.onScopeChanged(null, false, 27);
        assertEquals(List.of("open:27", "close:27"), events);
        assertEquals(27, top.get());
    }
}
