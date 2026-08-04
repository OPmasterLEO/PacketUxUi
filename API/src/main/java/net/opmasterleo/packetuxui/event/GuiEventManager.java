package net.opmasterleo.packetuxui.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Listener bus with a cached ordered snapshot — no sort/alloc on every click.
 */
public final class GuiEventManager {

    private static final Comparator<GuiListener> BY_PRIORITY =
            Comparator.comparingInt(l -> l.priority().ordinal());
    private static final GuiListener[] EMPTY = new GuiListener[0];

    private final CopyOnWriteArrayList<GuiListener> listeners = new CopyOnWriteArrayList<>();
    private volatile GuiListener[] ordered = EMPTY;

    public void register(GuiListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (listeners.addIfAbsent(listener)) {
            rebuildOrdered();
        }
    }

    public void unregister(GuiListener listener) {
        if (listener != null && listeners.remove(listener)) {
            rebuildOrdered();
        }
    }

    public void clear() {
        listeners.clear();
        ordered = EMPTY;
    }

    public boolean hasListeners() {
        return ordered.length > 0;
    }

    public List<GuiListener> listeners() {
        return List.of(ordered);
    }

    public void fireOpen(GuiOpenEvent event) {
        GuiListener[] snap = ordered;
        for (GuiListener listener : snap) {
            try {
                listener.onOpen(event);
            } catch (Throwable ignored) {
            }
        }
    }

    public void fireClose(GuiCloseEvent event) {
        GuiListener[] snap = ordered;
        for (GuiListener listener : snap) {
            try {
                listener.onClose(event);
            } catch (Throwable ignored) {
            }
        }
    }

    public void fireClick(GuiClickEvent event) {
        GuiListener[] snap = ordered;
        for (GuiListener listener : snap) {
            try {
                listener.onClick(event);
            } catch (Throwable ignored) {
            }
        }
    }

    public void fireClickPost(GuiClickPostEvent event) {
        GuiListener[] snap = ordered;
        for (int i = snap.length - 1; i >= 0; i--) {
            try {
                snap[i].onClickPost(event);
            } catch (Throwable ignored) {
            }
        }
    }

    public void fireDrag(GuiDragEvent event) {
        GuiListener[] snap = ordered;
        for (GuiListener listener : snap) {
            try {
                listener.onDrag(event);
            } catch (Throwable ignored) {
            }
        }
    }

    private void rebuildOrdered() {
        if (listeners.isEmpty()) {
            ordered = EMPTY;
            return;
        }
        ArrayList<GuiListener> copy = new ArrayList<>(listeners);
        copy.sort(BY_PRIORITY);
        ordered = copy.toArray(GuiListener[]::new);
    }
}
