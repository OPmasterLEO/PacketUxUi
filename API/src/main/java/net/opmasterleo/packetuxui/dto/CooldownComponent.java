package net.opmasterleo.packetuxui.dto;

import java.util.function.Consumer;

import net.opmasterleo.packetuxui.types.ExecuteComponent;

public final class CooldownComponent {

    private final long delay;
    private final Consumer<ExecuteComponent> execute;
    private final long freeze;
    private long expireTime;
    private long expireFreeze;

    public CooldownComponent() {
        this(0L, null, 0L);
    }

    public CooldownComponent(long delay) {
        this(delay, null, 0L);
    }

    public CooldownComponent(long delay, Consumer<ExecuteComponent> execute) {
        this(delay, execute, 0L);
    }

    public CooldownComponent(long delay, Consumer<ExecuteComponent> execute, long freeze) {
        this.delay = delay;
        this.execute = execute;
        this.freeze = freeze;
    }

    public long delay() {
        return delay;
    }

    public Consumer<ExecuteComponent> execute() {
        return execute;
    }

    public long freeze() {
        return freeze;
    }

    public CooldownComponent combine(CooldownComponent other) {
        Consumer<ExecuteComponent> combinedExecute;
        if (this.execute != null && other.execute != null) {
            combinedExecute = this.delay >= other.delay ? this.execute : other.execute;
        } else {
            combinedExecute = this.execute != null ? this.execute : other.execute;
        }
        CooldownComponent combined = new CooldownComponent(
                Math.max(this.delay, other.delay),
                combinedExecute,
                Math.max(this.freeze, other.freeze)
        );
        combined.expireTime = Math.max(this.expireTime, other.expireTime);
        combined.expireFreeze = Math.max(this.expireFreeze, other.expireFreeze);
        return combined;
    }

    public void resetFreeze() {
        expireFreeze = System.currentTimeMillis() + freeze;
    }

    public void resetTime() {
        expireTime = System.currentTimeMillis() + delay;
    }

    public boolean isFreezeExpired(long now) {
        return now >= expireFreeze;
    }

    public boolean isTimeExpired(long now) {
        return now >= expireTime;
    }
}
