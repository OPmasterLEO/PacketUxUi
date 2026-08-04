package net.opmasterleo.packetuxui.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.opmasterleo.packetuxui.service.MenuMode;
import net.opmasterleo.packetuxui.service.SlotKind;

class MenuBuildValidationTest {

    @Test
    void editableMenuFlagsUnsealedSlots() {
        MenuBuild build = MenuBuild.create()
                .rows(1)
                .mode(MenuMode.EDITABLE);

        LayoutDiagnostics diagnostics = build.validateLayout();
        assertFalse(diagnostics.ok());
    }

    @Test
    void sealingEditableMenuRemovesUnsealedWarnings() {
        MenuBuild build = MenuBuild.create()
                .rows(1)
                .mode(MenuMode.EDITABLE)
                .sealUnspecifiedTopSlots(SlotKind.DECORATIVE, null);

        LayoutDiagnostics diagnostics = build.validateLayout();
        assertTrue(diagnostics.ok());
    }

    @Test
    void layoutPlanDetectsCollisions() {
        MenuBuild build = MenuBuild.create().rows(1);
        LayoutDiagnostics diagnostics = build.validateLayout(new LayoutPlan(
                java.util.Set.of(0, 1),
                java.util.Set.of(1, 2),
                java.util.Set.of(),
                java.util.Set.of()
        ));
        assertFalse(diagnostics.ok());
    }
}
