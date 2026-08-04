package net.opmasterleo.packetuxui.nms.map;

/**
 * Fills ordinal lookup tables once. Hot path is {@code table[enum.ordinal()]},
 * not a switch or HashMap.
 */
public final class OrdinalMaps {

    private OrdinalMaps() {
    }

    @FunctionalInterface
    public interface EnumMapper<E extends Enum<E>, T> {
        T map(E value);
    }

    /** Writes {@code table[constant.ordinal()] = mapper.map(constant)} for every constant. */
    public static <E extends Enum<E>, T> void fill(E[] constants, T[] table, EnumMapper<E, T> mapper) {
        for (E constant : constants) {
            int i = constant.ordinal();
            if (i >= 0 && i < table.length) {
                table[i] = mapper.map(constant);
            }
        }
    }
}
