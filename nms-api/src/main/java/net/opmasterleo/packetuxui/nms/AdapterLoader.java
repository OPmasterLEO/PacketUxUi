package net.opmasterleo.packetuxui.nms;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;

public final class AdapterLoader {

    private static final String ADAPTER_PACKAGE = NmsAdapter.class.getPackageName();
    private static final String ADAPTER_SUFFIX = ".Adapter";

    private static final Map<String, String> MINECRAFT_TO_BUCKET;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("1.8", "v1_8_R1");
        map.put("1.8.3", "v1_8_R2");
        map.put("1.8.4", "v1_8_R3");
        map.put("1.8.5", "v1_8_R3");
        map.put("1.8.6", "v1_8_R3");
        map.put("1.8.7", "v1_8_R3");
        map.put("1.8.8", "v1_8_R3");
        map.put("1.8.9", "v1_8_R3");
        map.put("1.9", "v1_9_R1");
        map.put("1.9.1", "v1_9_R1");
        map.put("1.9.2", "v1_9_R1");
        map.put("1.9.3", "v1_9_R2");
        map.put("1.9.4", "v1_9_R2");
        map.put("1.10", "v1_10_R1");
        map.put("1.10.1", "v1_10_R1");
        map.put("1.10.2", "v1_10_R1");
        map.put("1.11", "v1_11_R1");
        map.put("1.11.1", "v1_11_R1");
        map.put("1.11.2", "v1_11_R1");
        map.put("1.12", "v1_12_R1");
        map.put("1.12.1", "v1_12_R1");
        map.put("1.12.2", "v1_12_R1");
        map.put("1.13", "v1_13_R1");
        map.put("1.13.1", "v1_13_R1");
        map.put("1.13.2", "v1_13_R2");
        map.put("1.14", "v1_14_R1");
        map.put("1.14.1", "v1_14_R1");
        map.put("1.14.2", "v1_14_R1");
        map.put("1.14.3", "v1_14_R1");
        map.put("1.14.4", "v1_14_R1");
        map.put("1.15", "v1_15_R1");
        map.put("1.15.1", "v1_15_R1");
        map.put("1.15.2", "v1_15_R1");
        map.put("1.16", "v1_16_R1");
        map.put("1.16.1", "v1_16_R1");
        map.put("1.16.2", "v1_16_R2");
        map.put("1.16.3", "v1_16_R2");
        map.put("1.16.4", "v1_16_R3");
        map.put("1.16.5", "v1_16_R3");
        map.put("1.17", "v1_17_R1");
        map.put("1.17.1", "v1_17_R1");
        map.put("1.18", "v1_18_R1");
        map.put("1.18.1", "v1_18_R1");
        map.put("1.18.2", "v1_18_R2");
        map.put("1.19", "v1_19_R1");
        map.put("1.19.1", "v1_19_R1");
        map.put("1.19.2", "v1_19_R1");
        map.put("1.19.3", "v1_19_R2");
        map.put("1.19.4", "v1_19_R3");
        map.put("1.20", "v1_20_R1");
        map.put("1.20.1", "v1_20_R1");
        map.put("1.20.2", "v1_20_R2");
        map.put("1.20.3", "v1_20_R3");
        map.put("1.20.4", "v1_20_R3");
        map.put("1.20.5", "v1_20_R4");
        map.put("1.20.6", "v1_20_R4");
        map.put("1.21", "v1_21_R1");
        map.put("1.21.1", "v1_21_R1");
        map.put("1.21.2", "v1_21_R2");
        map.put("1.21.3", "v1_21_R2");
        map.put("1.21.4", "v1_21_R3");
        map.put("1.21.5", "v1_21_R4");
        map.put("1.21.6", "v1_21_R5");
        map.put("1.21.7", "v1_21_R5");
        map.put("1.21.8", "v1_21_R5");
        map.put("1.21.9", "v1_21_R6");
        map.put("1.21.10", "v1_21_R6");
        map.put("1.21.11", "v1_21_R7");
        map.put("26.1", "v26_1");
        map.put("26.2", "v26_2");
        MINECRAFT_TO_BUCKET = Collections.unmodifiableMap(map);
    }

    private AdapterLoader() {
    }

    public static NmsAdapter load() {
        String bucket = resolveBucket();
        String className = ADAPTER_PACKAGE + "." + bucket + ADAPTER_SUFFIX;
        try {
            Class<?> clazz = Class.forName(className);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            if (instance instanceof NmsAdapter adapter) {
                return adapter;
            }
            throw new NmsUnsupportedException(className + " does not implement NmsAdapter");
        } catch (ClassNotFoundException error) {
            throw new NmsUnsupportedException(
                    "No NMS adapter for bucket " + bucket + " (server " + describeServer() + ")",
                    error
            );
        } catch (NmsUnsupportedException error) {
            throw error;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            throw new NmsUnsupportedException(
                    "Failed to initialize NMS adapter " + bucket + " (server " + describeServer() + ")",
                    error
            );
        }
    }

    public static String resolveBucket() {
        String craftBukkitPackage = Bukkit.getServer().getClass().getPackage().getName();
        if (craftBukkitPackage.contains(".v")) {
            String[] parts = craftBukkitPackage.split("\\.");
            if (parts.length >= 4 && parts[3].startsWith("v")) {
                return parts[3];
            }
        }

        String minecraftVersion = Bukkit.getBukkitVersion().split("-")[0];
        String mapped = MINECRAFT_TO_BUCKET.get(minecraftVersion);
        if (mapped != null) {
            return mapped;
        }

        throw new NmsUnsupportedException(
                "Unsupported Minecraft version \"" + minecraftVersion
                        + "\" (craftbukkit package " + craftBukkitPackage + ")"
        );
    }

    private static String describeServer() {
        try {
            return Bukkit.getBukkitVersion() + " / " + Bukkit.getServer().getClass().getPackage().getName();
        } catch (Throwable ignored) {
            return "unknown";
        }
    }
}
