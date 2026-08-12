package net.opmasterleo.packetuxui.service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.opmasterleo.packetuxui.common.StringUtils;

public final class SignView {

    private final Component[] lines;
    private final String materialName;
    private final String dyeColor;
    private final boolean glow;
    private final Location location;
    private final SignFinishHandler handler;

    public SignView(
            Component[] lines,
            String materialName,
            String dyeColor,
            boolean glow,
            Location location,
            SignFinishHandler handler
    ) {
        this.lines = pad(lines);
        this.materialName = materialName == null || materialName.isEmpty()
                ? defaultMaterialName()
                : materialName;
        this.dyeColor = dyeColor == null || dyeColor.isEmpty() ? DyeColor.BLACK.name() : dyeColor;
        this.glow = glow;
        this.location = location == null ? null : location.clone();
        this.handler = handler;
    }

    public Component[] lines() {
        return lines.clone();
    }

    public String materialName() {
        return materialName;
    }

    public String dyeColor() {
        return dyeColor;
    }

    public boolean glow() {
        return glow;
    }

    public Location location() {
        return location == null ? null : location.clone();
    }

    public SignFinishHandler handler() {
        return handler;
    }

    public String[] legacyLines() {
        String[] out = new String[4];
        for (int i = 0; i < 4; i++) {
            out[i] = LegacyComponentSerializer.legacySection().serialize(lines[i]);
        }
        return out;
    }

    public SignView withLines(Component[] next) {
        return new SignView(next, materialName, dyeColor, glow, location, handler);
    }

    public static Component parseLine(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        if (input.indexOf('§') >= 0) {
            return LegacyComponentSerializer.legacySection().deserialize(input);
        }
        if (input.indexOf('&') >= 0 && input.indexOf('<') < 0) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
        }
        return StringUtils.toComponent(input);
    }

    public static String defaultMaterialName() {
        try {
            if (Material.getMaterial("OAK_WALL_SIGN") != null) {
                return "OAK_WALL_SIGN";
            }
            if (Material.getMaterial("WALL_SIGN") != null) {
                return "WALL_SIGN";
            }
            if (Material.getMaterial("SIGN_POST") != null) {
                return "SIGN_POST";
            }
        } catch (Throwable ignored) {
        }
        return "OAK_WALL_SIGN";
    }

    private static Component[] pad(Component[] raw) {
        Component[] out = new Component[4];
        for (int i = 0; i < 4; i++) {
            Component line = raw != null && i < raw.length ? raw[i] : null;
            out[i] = line == null ? Component.empty() : line;
        }
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SignView other)) {
            return false;
        }
        return glow == other.glow
                && Arrays.equals(lines, other.lines)
                && Objects.equals(materialName, other.materialName)
                && Objects.equals(dyeColor, other.dyeColor)
                && Objects.equals(location, other.location)
                && Objects.equals(handler, other.handler);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(lines), materialName, dyeColor, glow, location, handler);
    }

    public List<Component> lineList() {
        return List.of(lines);
    }
}
