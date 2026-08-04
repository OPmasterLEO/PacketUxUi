package net.opmasterleo.packetuxui.event;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.service.MenuMode;
import net.opmasterleo.packetuxui.types.InventoryType;

/**
 * Packet analogue of Bukkit {@code InventoryView} for an open virtual menu.
 */
public final class GuiView {

    private final int windowId;
    private final Menu menu;
    private final int topSlotCount;
    private final int stateId;

    public GuiView(int windowId, Menu menu, int topSlotCount, int stateId) {
        this.windowId = windowId;
        this.menu = menu;
        this.topSlotCount = topSlotCount;
        this.stateId = stateId;
    }

    public int windowId() {
        return windowId;
    }

    public Menu menu() {
        return menu;
    }

    public Component title() {
        return menu == null ? Component.empty() : menu.name();
    }

    public InventoryType type() {
        return menu == null ? InventoryType.GENERIC9X3 : menu.type();
    }

    public MenuMode mode() {
        return menu == null ? MenuMode.READ_ONLY : menu.mode();
    }

    public int topSlotCount() {
        return topSlotCount;
    }

    public int bottomSlotCount() {
        return 36;
    }

    public int totalSlotCount() {
        return topSlotCount + 36;
    }

    public int stateId() {
        return stateId;
    }
}
