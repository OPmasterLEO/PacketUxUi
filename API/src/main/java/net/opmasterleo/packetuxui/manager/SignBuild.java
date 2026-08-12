package net.opmasterleo.packetuxui.manager;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.service.SignAction;
import net.opmasterleo.packetuxui.service.SignFinishHandler;
import net.opmasterleo.packetuxui.service.SignResult;
import net.opmasterleo.packetuxui.service.SignView;

public final class SignBuild {

    private final Component[] lines = {
            Component.empty(), Component.empty(), Component.empty(), Component.empty()
    };
    private String materialName = SignView.defaultMaterialName();
    private String dyeColor = DyeColor.BLACK.name();
    private boolean glow;
    private Location location;
    private SignFinishHandler handler;

    public static SignBuild create() {
        return new SignBuild();
    }

    public SignBuild lines(String... miniMessageOrLegacy) {
        for (int i = 0; i < 4; i++) {
            String raw = miniMessageOrLegacy != null && i < miniMessageOrLegacy.length
                    ? miniMessageOrLegacy[i]
                    : null;
            lines[i] = raw == null ? Component.empty() : SignView.parseLine(raw);
        }
        return this;
    }

    public SignBuild lines(Component... components) {
        for (int i = 0; i < 4; i++) {
            Component line = components != null && i < components.length ? components[i] : null;
            lines[i] = line == null ? Component.empty() : line;
        }
        return this;
    }

    public SignBuild line(int index, String miniMessageOrLegacy) {
        checkIndex(index);
        lines[index] = miniMessageOrLegacy == null
                ? Component.empty()
                : SignView.parseLine(miniMessageOrLegacy);
        return this;
    }

    public SignBuild line(int index, Component line) {
        checkIndex(index);
        lines[index] = line == null ? Component.empty() : line;
        return this;
    }

    public SignBuild type(Material type) {
        if (type == null) {
            throw new IllegalArgumentException("sign type cannot be null");
        }
        this.materialName = type.name();
        return this;
    }

    public SignBuild type(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            throw new IllegalArgumentException("sign type cannot be empty");
        }
        this.materialName = materialName;
        return this;
    }

    public SignBuild color(DyeColor color) {
        this.dyeColor = color == null ? DyeColor.BLACK.name() : color.name();
        return this;
    }

    public SignBuild glow(boolean glow) {
        this.glow = glow;
        return this;
    }

    public SignBuild location(Location location) {
        this.location = location == null ? null : location.clone();
        return this;
    }

    public SignBuild onFinish(SignFinishHandler handler) {
        this.handler = handler;
        return this;
    }

    public SignBuild onComplete(java.util.function.BiConsumer<Player, SignResult> consumer) {
        if (consumer == null) {
            this.handler = null;
            return this;
        }
        this.handler = (player, result) -> {
            consumer.accept(player, result);
            return SignAction.close();
        };
        return this;
    }

    public SignView build() {
        return new SignView(lines.clone(), materialName, dyeColor, glow, location, handler);
    }

    public void open(Player player) {
        PacketGuiManager.ofApi().openSign(player, build());
    }

    private static void checkIndex(int index) {
        if (index < 0 || index > 3) {
            throw new IndexOutOfBoundsException("sign line index must be 0..3");
        }
    }
}
