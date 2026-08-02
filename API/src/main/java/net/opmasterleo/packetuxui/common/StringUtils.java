package net.opmasterleo.packetuxui.common;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class StringUtils {

    private StringUtils() {
    }

    public static Component toComponent(String input) {
        return MiniMessage.miniMessage().deserialize(input);
    }

    public static String toPlain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
