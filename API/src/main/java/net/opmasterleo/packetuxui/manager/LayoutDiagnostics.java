package net.opmasterleo.packetuxui.manager;

import java.util.List;

public record LayoutDiagnostics(List<LayoutIssue> issues) {
    public boolean ok() {
        return issues == null || issues.isEmpty();
    }
}
