package net.opmasterleo.packetuxui.nms.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;

class UxItemNativePreserveTest {

    @Test
    void builderItemsKeepLoreEnchantsWithoutNativeStack() {
        UxItem item = UxItem.builder("minecraft:stone")
                .amount(4)
                .name(Component.text("Stone"))
                .lore(Component.text("line"))
                .enchantment("minecraft:sharpness", 2, true)
                .build();
        assertFalse(item.hasNativeBukkit());
        assertEquals(4, item.amount());
        assertEquals(1, item.lore().size());
        assertEquals(2, item.enchantments().get("minecraft:sharpness"));
        assertFalse(item.hideEnchantments());
    }
}
