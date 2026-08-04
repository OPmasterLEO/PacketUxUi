package net.opmasterleo.packetuxui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.types.InventoryType;

/**
 * Pure decision logic for present(): same type+mode diffs; otherwise silent swap while open.
 */
class PresentDecisionTest {

    enum Action {
        DIFF,
        SWAP,
        OPEN
    }

    static Action decide(MenuSession existing, Menu next) {
        if (existing != null && existing.phase() == SessionPhase.OPEN) {
            if (existing.menu().mode() == next.mode()
                    && existing.menu().type() == next.type()) {
                return Action.DIFF;
            }
            return Action.SWAP;
        }
        return Action.OPEN;
    }

    private static Menu menu(InventoryType type, MenuMode mode) {
        return new Menu(Component.text("t"), type, Map.of(), null, mode, null);
    }

    @Test
    void sameTypeAndModeDiffs() {
        Menu chest = menu(InventoryType.GENERIC9X3, MenuMode.READ_ONLY);
        MenuSession session = new MenuSession(chest, 1);
        assertEquals(Action.DIFF, decide(session, menu(InventoryType.GENERIC9X3, MenuMode.READ_ONLY)));
    }

    @Test
    void differentTypeSilentSwapsEvenIfModeDiffers() {
        Menu chest = menu(InventoryType.GENERIC9X3, MenuMode.EDITABLE);
        MenuSession session = new MenuSession(chest, 1);
        assertEquals(Action.SWAP, decide(session, menu(InventoryType.HOPPER, MenuMode.READ_ONLY)));
        assertEquals(Action.SWAP, decide(session, menu(InventoryType.GENERIC9X6, MenuMode.EDITABLE)));
    }

    @Test
    void noSessionOpens() {
        assertEquals(Action.OPEN, decide(null, menu(InventoryType.HOPPER, MenuMode.READ_ONLY)));
    }
}
