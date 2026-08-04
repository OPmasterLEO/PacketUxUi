package net.opmasterleo.packetuxui.manager;

import java.util.Set;

public record LayoutPlan(
        Set<Integer> contentSlots,
        Set<Integer> actionSlots,
        Set<Integer> footerSlots,
        Set<Integer> navigationSlots
) {
}
