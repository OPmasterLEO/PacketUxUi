package net.opmasterleo.packetuxui.nms;

import net.opmasterleo.packetuxui.nms.item.UxItem;

public interface ItemBridge {

    Object toNms(UxItem item);

    UxItem fromNms(Object nmsItem);

    UxItem empty();

    boolean isEmpty(UxItem item);
}
