package net.opmasterleo.packetuxui.manager;

public record AsyncMenuResult(
        AsyncMenuStatus status,
        String reason,
        int expectedGeneration,
        int observedGeneration
) {
    public static AsyncMenuResult ok(int expected, int observed) {
        return new AsyncMenuResult(AsyncMenuStatus.APPLIED, "applied", expected, observed);
    }

    public static AsyncMenuResult fail(AsyncMenuStatus status, String reason, int expected, int observed) {
        return new AsyncMenuResult(status, reason, expected, observed);
    }
}
