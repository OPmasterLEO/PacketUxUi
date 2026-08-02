package net.opmasterleo.packetuxui.nms;

public final class ProtocolProbe {

    private ProtocolProbe() {
    }

    public static int current() {
        try {
            Class<?> shared = Class.forName("net.minecraft.SharedConstants");
            Object version = shared.getMethod("getCurrentVersion").invoke(null);
            try {
                Object protocol = version.getClass().getMethod("getProtocolVersion").invoke(version);
                if (protocol instanceof Integer i) {
                    return i;
                }
            } catch (NoSuchMethodException ignored) {
            }
            try {
                Object protocol = version.getClass().getMethod("protocolVersion").invoke(version);
                if (protocol instanceof Integer i) {
                    return i;
                }
            } catch (NoSuchMethodException ignored) {
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object server = bukkit.getMethod("getServer").invoke(null);
            String version = String.valueOf(server.getClass().getPackage().getName());
            int idx = version.lastIndexOf('.');
            if (idx >= 0) {
                String nms = version.substring(idx + 1);
                Integer mapped = mapLegacyPackage(nms);
                if (mapped != null) {
                    return mapped;
                }
            }
        } catch (Throwable ignored) {
        }
        throw new UnsupportedClassVersionError("Cannot probe protocol version");
    }

    private static Integer mapLegacyPackage(String nms) {
        return switch (nms) {
            case "v1_8_R1", "v1_8_R2", "v1_8_R3" -> ProtocolVersions.V1_8;
            case "v1_9_R1" -> ProtocolVersions.V1_9;
            case "v1_9_R2" -> ProtocolVersions.V1_9_4;
            case "v1_10_R1" -> ProtocolVersions.V1_10;
            case "v1_11_R1" -> ProtocolVersions.V1_11;
            case "v1_12_R1" -> ProtocolVersions.V1_12_2;
            case "v1_13_R1" -> ProtocolVersions.V1_13;
            case "v1_13_R2" -> ProtocolVersions.V1_13_2;
            case "v1_14_R1" -> ProtocolVersions.V1_14_4;
            case "v1_15_R1" -> ProtocolVersions.V1_15_2;
            case "v1_16_R1" -> ProtocolVersions.V1_16_1;
            case "v1_16_R2" -> ProtocolVersions.V1_16_3;
            case "v1_16_R3" -> ProtocolVersions.V1_16_4;
            default -> null;
        };
    }
}
