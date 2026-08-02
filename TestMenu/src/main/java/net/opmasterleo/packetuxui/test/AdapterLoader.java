package net.opmasterleo.packetuxui.test;

import net.opmasterleo.packetuxui.nms.NmsAdapter;

public final class AdapterLoader {

    private static final String[] CANDIDATES = {
            "net.opmasterleo.packetuxui.nms.v26_2.Adapter",
            "net.opmasterleo.packetuxui.nms.v26_1.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_21_R7.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_21_R6.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_21_R5.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_21_R4.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_21_R3.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_21_R2.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_21_R1.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_20_R4.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_20_R3.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_20_R2.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_20_R1.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_19_R3.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_19_R2.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_19_R1.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_18_R2.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_18_R1.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_17_R1.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_16_R3.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_16_R2.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_16_R1.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_15_R1.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_14_R1.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_13_R2.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_13_R1.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_12_R1.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_11_R1.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_10_R1.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_9_R2.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_9_R1.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_8_R3.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_8_R2.Adapter",
            "net.opmasterleo.packetuxui.nms.v1_8_R1.Adapter"
    };

    private AdapterLoader() {
    }

    public static NmsAdapter load() {
        Throwable last = null;
        for (String name : CANDIDATES) {
            try {
                Class<?> clazz = Class.forName(name);
                Object instance = clazz.getConstructor().newInstance();
                if (instance instanceof NmsAdapter adapter) {
                    return adapter;
                }
            } catch (Throwable error) {
                last = error;
            }
        }
        UnsupportedOperationException failure = new UnsupportedOperationException(
                "No compatible NMS adapter for this Minecraft version");
        if (last != null) {
            failure.initCause(last);
        }
        throw failure;
    }
}
