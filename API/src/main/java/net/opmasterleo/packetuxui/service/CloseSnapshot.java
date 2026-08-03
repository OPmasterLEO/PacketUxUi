package net.opmasterleo.packetuxui.service;

import java.util.List;

import net.opmasterleo.packetuxui.nms.item.UxItem;

public final class CloseSnapshot {

    private final List<UxItem> top;
    private final UxItem cursor;

    public CloseSnapshot(List<UxItem> top, UxItem cursor) {
        this.top = List.copyOf(top);
        this.cursor = cursor == null || cursor.isEmpty() ? UxItem.EMPTY : cursor;
    }

    public List<UxItem> top() {
        return top;
    }

    public UxItem cursor() {
        return cursor;
    }
}
