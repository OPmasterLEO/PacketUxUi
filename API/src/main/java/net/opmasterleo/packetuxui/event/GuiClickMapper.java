package net.opmasterleo.packetuxui.event;

import java.util.Objects;

import net.opmasterleo.packetuxui.nms.ClickPacket;
import net.opmasterleo.packetuxui.nms.WindowClickType;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.types.ButtonType;
import net.opmasterleo.packetuxui.types.ClickData;
import net.opmasterleo.packetuxui.types.ClickType;

/**
 * Maps raw {@link ClickPacket} + menu context → Bukkit-like click metadata.
 */
public final class GuiClickMapper {

    private GuiClickMapper() {
    }

    public static GuiSlotType slotType(int slot, int topSize) {
        return slotType(slot, topSize, 36);
    }

    public static GuiSlotType slotType(int slot, int topSize, int bottomSize) {
        if (slot == -999 || slot < 0) {
            return GuiSlotType.OUTSIDE;
        }
        if (slot < topSize) {
            return GuiSlotType.CONTAINER;
        }
        if (bottomSize <= 0) {
            return GuiSlotType.OUTSIDE;
        }
        int bottom = slot - topSize;
        if (bottom >= 27 && bottom < bottomSize) {
            return GuiSlotType.HOTBAR;
        }
        if (bottom >= 0 && bottom < bottomSize) {
            return GuiSlotType.PLAYER;
        }
        return GuiSlotType.OUTSIDE;
    }

    public static UxItem currentItem(Menu menu, int slot, int topSize) {
        if (menu == null || slot < 0 || slot >= topSize) {
            return UxItem.EMPTY;
        }
        var items = menu.items();
        if (slot >= items.size()) {
            return UxItem.EMPTY;
        }
        UxItem item = items.get(slot);
        return item == null ? UxItem.EMPTY : item;
    }

    public static int hotbarButton(ClickPacket packet, ButtonType buttonType) {
        if (buttonType == null) {
            return -1;
        }
        return switch (buttonType) {
            case NUM_1 -> 0;
            case NUM_2 -> 1;
            case NUM_3 -> 2;
            case NUM_4 -> 3;
            case NUM_5 -> 4;
            case NUM_6 -> 5;
            case NUM_7 -> 6;
            case NUM_8 -> 7;
            case NUM_9 -> 8;
            default -> packet != null && packet.clickType() == WindowClickType.SWAP
                    ? packet.button()
                    : -1;
        };
    }

    public static GuiClickAction action(
            ClickPacket packet,
            ClickData clickData,
            UxItem current,
            UxItem carried
    ) {
        Objects.requireNonNull(packet, "packet");
        WindowClickType wct = packet.clickType();
        boolean slotEmpty = current == null || current.isEmpty();
        boolean cursorEmpty = carried == null || carried.isEmpty();
        ClickType ct = clickData == null ? ClickType.UNDEFINED : clickData.clickType();
        ButtonType bt = clickData == null ? ButtonType.LEFT : clickData.buttonType();

        return switch (wct) {
            case QUICK_MOVE -> GuiClickAction.MOVE_TO_OTHER_INVENTORY;
            case SWAP -> GuiClickAction.HOTBAR_SWAP;
            case CLONE -> GuiClickAction.CLONE_STACK;
            case PICKUP_ALL -> GuiClickAction.COLLECT_TO_CURSOR;
            case THROW -> {
                if (packet.slot() == -999) {
                    yield packet.button() == 0 ? GuiClickAction.DROP_ONE_CURSOR : GuiClickAction.DROP_ALL_CURSOR;
                }
                yield packet.button() == 0 ? GuiClickAction.DROP_ONE_SLOT : GuiClickAction.DROP_ALL_SLOT;
            }
            case PICKUP -> {
                if (packet.slot() == -999) {
                    yield GuiClickAction.NOTHING;
                }
                if (ct == ClickType.PLACE || (!cursorEmpty && slotEmpty)) {
                    yield bt == ButtonType.RIGHT ? GuiClickAction.PLACE_ONE : GuiClickAction.PLACE_ALL;
                }
                if (!cursorEmpty && !slotEmpty) {
                    yield GuiClickAction.SWAP_WITH_CURSOR;
                }
                if (cursorEmpty && !slotEmpty) {
                    yield bt == ButtonType.RIGHT ? GuiClickAction.PICKUP_HALF : GuiClickAction.PICKUP_ALL;
                }
                yield GuiClickAction.NOTHING;
            }
            case QUICK_CRAFT -> GuiClickAction.UNKNOWN;
            default -> GuiClickAction.UNKNOWN;
        };
    }
}
