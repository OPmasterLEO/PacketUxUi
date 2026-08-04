package net.opmasterleo.packetuxui.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Listener bus with cached ordered snapshots — no sort/alloc on the hot path.
 * Open hooks use a dedicated list so click-only {@link GuiListener}s do not force
 * {@link GuiOpenEvent} allocation.
 */
public final class GuiEventManager {

    private static final Comparator<GuiListener> BY_PRIORITY = new PriorityComparator();
    private static final GuiListener[] EMPTY = new GuiListener[0];
    private static final GuiOpenListener[] EMPTY_OPEN = new GuiOpenListener[0];

    private final CopyOnWriteArrayList<GuiListener> listeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<GuiOpenListener> openListeners = new CopyOnWriteArrayList<>();
    private volatile GuiListener[] ordered = EMPTY;
    /** GuiListeners that override {@link GuiListener#onOpen}. */
    private volatile GuiListener[] orderedOpen = EMPTY;
    private volatile GuiOpenListener[] openOnly = EMPTY_OPEN;

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

    /**
     * Register an open-only hook (InventoryOpenEvent analogue). Preferred when you do not
     * need click/close — empty registration costs nothing on open.
     */
    public void registerOpen(GuiOpenListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (openListeners.addIfAbsent(listener)) {
            rebuildOpenOnly();
        }
    }

    public void unregisterOpen(GuiOpenListener listener) {
        if (listener != null && openListeners.remove(listener)) {
            rebuildOpenOnly();
        }
    }

    public void clear() {
        listeners.clear();
        openListeners.clear();
        ordered = EMPTY;
        orderedOpen = EMPTY;
        openOnly = EMPTY_OPEN;
    }

    public boolean hasListeners() {
        return ordered.length > 0;
    }

    /** True if anyone cares about opens (dedicated hooks or GuiListener#onOpen). */
    public boolean hasOpenListeners() {
        return openOnly.length > 0 || orderedOpen.length > 0;
    }

    public List<GuiListener> listeners() {
        return List.of(ordered);
    }

    public void fireOpen(GuiOpenEvent event) {
        GuiOpenListener[] opens = openOnly;
        GuiListener[] guiOpens = orderedOpen;
        if (opens.length == 0 && guiOpens.length == 0) {
            return;
        }
        for (GuiOpenListener listener : opens) {
            try {
                listener.onInventoryOpen(event);
            } catch (Throwable ignored) {
            }
        }
        for (GuiListener listener : guiOpens) {
            try {
                listener.onOpen(event);
            } catch (Throwable ignored) {
            }
        }
    }

    public void fireClose(GuiCloseEvent event) {
        GuiListener[] snap = ordered;
        if (snap.length == 0) {
            return;
        }
        for (GuiListener listener : snap) {
            try {
                listener.onClose(event);
            } catch (Throwable ignored) {
            }
        }
    }

    public void fireClick(GuiClickEvent event) {
        GuiListener[] snap = ordered;
        if (snap.length == 0) {
            return;
        }
        for (GuiListener listener : snap) {
            try {
                listener.onClick(event);
            } catch (Throwable ignored) {
            }
        }
    }

    public void fireClickPost(GuiClickPostEvent event) {
        GuiListener[] snap = ordered;
        if (snap.length == 0) {
            return;
        }
        for (int i = snap.length - 1; i >= 0; i--) {
            try {
                snap[i].onClickPost(event);
            } catch (Throwable ignored) {
            }
        }
    }

    public void fireDrag(GuiDragEvent event) {
        GuiListener[] snap = ordered;
        if (snap.length == 0) {
            return;
        }
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
            orderedOpen = EMPTY;
            return;
        }
        ArrayList<GuiListener> copy = new ArrayList<>(listeners);
        copy.sort(BY_PRIORITY);
        ordered = copy.toArray(EMPTY);
        ArrayList<GuiListener> opens = new ArrayList<>(copy.size());
        for (GuiListener listener : copy) {
            if (overridesOnOpen(listener)) {
                opens.add(listener);
            }
        }
        orderedOpen = opens.isEmpty() ? EMPTY : opens.toArray(EMPTY);
    }

    private void rebuildOpenOnly() {
        if (openListeners.isEmpty()) {
            openOnly = EMPTY_OPEN;
            return;
        }
        openOnly = openListeners.toArray(EMPTY_OPEN);
    }

    /** True when the listener type overrides the default no-op {@link GuiListener#onOpen}. */
    private static boolean overridesOnOpen(GuiListener listener) {
        Class<?> type = listener.getClass();
        while (type != null && type != Object.class) {
            try {
                type.getDeclaredMethod("onOpen", GuiOpenEvent.class);
                return type != GuiListener.class;
            } catch (NoSuchMethodException ignored) {
                type = type.getSuperclass();
            }
        }
        return false;
    }

    private static final class PriorityComparator implements Comparator<GuiListener> {
        @Override
        public int compare(GuiListener left, GuiListener right) {
            return Integer.compare(left.priority().ordinal(), right.priority().ordinal());
        }
    }
}
