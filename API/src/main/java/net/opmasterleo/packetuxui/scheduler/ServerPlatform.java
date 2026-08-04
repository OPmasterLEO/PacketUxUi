package net.opmasterleo.packetuxui.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ServerPlatform {

    private final JavaPlugin plugin;
    private final SchedulerKind kind;
    private final boolean folia;

    private ServerPlatform(JavaPlugin plugin, SchedulerKind kind, boolean folia) {
        this.plugin = plugin;
        this.kind = kind;
        this.folia = folia;
    }

    public static ServerPlatform detect(JavaPlugin plugin) {
        boolean paper = detectPaperSchedulers();
        boolean folia = paper && detectFolia();
        return new ServerPlatform(plugin, paper ? SchedulerKind.PAPER : SchedulerKind.BUKKIT, folia);
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public SchedulerKind kind() {
        return kind;
    }

    public boolean isPaper() {
        return kind == SchedulerKind.PAPER;
    }

    public boolean isBukkit() {
        return kind == SchedulerKind.BUKKIT;
    }

    public boolean isFolia() {
        return folia;
    }

    /** @deprecated use {@link #isPaper()} */
    @Deprecated
    public boolean hasPaperSchedulers() {
        return isPaper();
    }

    /**
     * Tick delay for schedules that allow zero (Folia {@code execute} / immediate).
     * Negative values become 0.
     */
    public static long delayTicks(long ticks) {
        return Math.max(0L, ticks);
    }

    /**
     * Tick period / positive delay — Folia {@code runDelayed}/{@code runAtFixedRate} require &gt; 0.
     * Prefer {@link #delayTicks(long)} when zero-delay immediate/next-tick is intended.
     */
    public static long periodTicks(long ticks) {
        return Math.max(1L, ticks);
    }

    /** @deprecated use {@link #periodTicks(long)} */
    @Deprecated
    public static long ticks(long ticks) {
        return periodTicks(ticks);
    }

    private static boolean detectPaperSchedulers() {
        try {
            Class<?> bukkit = Bukkit.class;
            bukkit.getMethod("getRegionScheduler");
            bukkit.getMethod("getGlobalRegionScheduler");
            bukkit.getMethod("getAsyncScheduler");
            return true;
        } catch (NoSuchMethodException | NoClassDefFoundError error) {
            return false;
        }
    }

    private static boolean detectFolia() {
        try {
            Class<?> buildInfo = Class.forName("io.papermc.paper.ServerBuildInfo");
            Object info = buildInfo.getMethod("buildInfo").invoke(null);
            Class<?> keyClass = Class.forName("net.kyori.adventure.key.Key");
            Object foliaBrand = keyClass.getMethod("key", String.class, String.class)
                    .invoke(null, "papermc", "folia");
            Boolean compatible = (Boolean) info.getClass()
                    .getMethod("isBrandCompatible", keyClass)
                    .invoke(info, foliaBrand);
            if (Boolean.TRUE.equals(compatible)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError error) {
            return false;
        }
    }
}
