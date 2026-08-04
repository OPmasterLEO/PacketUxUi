package net.opmasterleo.packetuxui.service;

import java.util.UUID;

public record ClickDecision(
        UUID playerId,
        int windowId,
        int slot,
        SlotKind slotKind,
        boolean handlerFound,
        boolean takeable,
        String result
) {
}
