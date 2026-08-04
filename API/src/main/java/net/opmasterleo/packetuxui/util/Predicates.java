package net.opmasterleo.packetuxui.util;

import java.util.function.Predicate;

/** Static predicate constants — avoid {@code s -> true} lambdas. */
public final class Predicates {

    public static final Predicate<Integer> ALWAYS_TRUE_INT = new AlwaysTrueInt();
    public static final Predicate<Object> ALWAYS_TRUE = new AlwaysTrue();

    private Predicates() {
    }

    private static final class AlwaysTrueInt implements Predicate<Integer> {
        @Override
        public boolean test(Integer value) {
            return true;
        }
    }

    private static final class AlwaysTrue implements Predicate<Object> {
        @Override
        public boolean test(Object value) {
            return true;
        }
    }
}
