package net.opmasterleo.packetuxui.service;

import java.util.UUID;

import net.kyori.adventure.text.Component;

public record MenuSessionDiagnostics(
        UUID playerId,
        int generation,
        SessionPhase phase,
        int windowId,
        Component title,
        boolean transitionActive,
        String lastClickDecision
) {
}
