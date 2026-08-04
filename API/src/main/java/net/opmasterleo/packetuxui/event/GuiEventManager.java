package net.opmasterleo.packetuxui.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Listener bus with cached ordered snapshots — no sort/alloc on the hot path.
 * Dedicated open/close/click/drag lists keep unrelated hooks from forcing event allocation.
 */
public final class GuiEventManager {

    private static final Comparator<GuiListener> BY_PRIORITY = new PriorityComparator();
    private static final GuiListener[] EMPTY = new GuiListener[0];
    private static final GuiOpenListener[] EMPTY_OPEN = new GuiOpenListener[0];
    private static final GuiCloseListener[] EMPTY_CLOSE = new GuiCloseListener[0];
    private static final GuiClickListener[] EMPTY_CLICK = new GuiClickListener[0];
    private static final GuiDragListener[] EMPTY_DRAG = new GuiDragListener[0];

    private final CopyOnWriteArrayList<GuiListener> listeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<GuiOpenListener> openListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<GuiCloseListener> closeListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<GuiClickListener> clickListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<GuiDragListener> dragListeners = new CopyOnWriteArrayList<>();

    private volatile GuiListener[] ordered = EMPTY;
    private volatile GuiListener[] orderedOpen = EMPTY;
    private volatile GuiListener[] orderedClose = EMPTY;
    private volatile GuiListener[] orderedClick = EMPTY;
    private volatile GuiListener[] orderedClickPost = EMPTY;
    private volatile GuiListener[] orderedDrag = EMPTY;
    private volatile GuiOpenListener[] openOnly = EMPTY_OPEN;
    private volatile GuiCloseListener[] closeOnly = EMPTY_CLOSE;
    private volatile GuiClickListener[] clickOnly = EMPTY_CLICK;
    private volatile GuiDragListener[] dragOnly = EMPTY_DRAG;

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

    public void registerClose(GuiCloseListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (closeListeners.addIfAbsent(listener)) {
            rebuildCloseOnly();
        }
    }

    public void unregisterClose(GuiCloseListener listener) {
        if (listener != null && closeListeners.remove(listener)) {
            rebuildCloseOnly();
        }
    }

    public void registerClick(GuiClickListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (clickListeners.addIfAbsent(listener)) {
            rebuildClickOnly();
        }
    }

    public void unregisterClick(GuiClickListener listener) {
        if (listener != null && clickListeners.remove(listener)) {
            rebuildClickOnly();
        }
    }

    public void registerDrag(GuiDragListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (dragListeners.addIfAbsent(listener)) {
            rebuildDragOnly();
        }
    }

    public void unregisterDrag(GuiDragListener listener) {
        if (listener != null && dragListeners.remove(listener)) {
            rebuildDragOnly();
        }
    }

    public void clear() {
        listeners.clear();
        openListeners.clear();
        closeListeners.clear();
        clickListeners.clear();
        dragListeners.clear();
        ordered = EMPTY;
        orderedOpen = EMPTY;
        orderedClose = EMPTY;
        orderedClick = EMPTY;
        orderedClickPost = EMPTY;
        orderedDrag = EMPTY;
        openOnly = EMPTY_OPEN;
        closeOnly = EMPTY_CLOSE;
        clickOnly = EMPTY_CLICK;
        dragOnly = EMPTY_DRAG;
    }

    public boolean hasListeners() {
        return ordered.length > 0;
    }

    public boolean hasOpenListeners() {
        return openOnly.length > 0 || orderedOpen.length > 0;
    }

    public boolean hasCloseListeners() {
        return closeOnly.length > 0 || orderedClose.length > 0;
    }

    public boolean hasClickListeners() {
        return clickOnly.length > 0 || orderedClick.length > 0;
    }

    public boolean hasDragListeners() {
        return dragOnly.length > 0 || orderedDrag.length > 0;
    }

    public boolean hasClickPostListeners() {
        return orderedClickPost.length > 0;
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
        GuiCloseListener[] closes = closeOnly;
        GuiListener[] guiCloses = orderedClose;
        if (closes.length == 0 && guiCloses.length == 0) {
            return;
        }
        for (GuiCloseListener listener : closes) {
            try {
                listener.onInventoryClose(event);
            } catch (Throwable ignored) {
            }
        }
        for (GuiListener listener : guiCloses) {
            try {
                listener.onClose(event);
            } catch (Throwable ignored) {
            }
        }
    }

    public void fireClick(GuiClickEvent event) {
        GuiClickListener[] clicks = clickOnly;
        GuiListener[] guiClicks = orderedClick;
        if (clicks.length == 0 && guiClicks.length == 0) {
            return;
        }
        for (GuiClickListener listener : clicks) {
            try {
                listener.onInventoryClick(event);
            } catch (Throwable ignored) {
            }
        }
        for (GuiListener listener : guiClicks) {
            try {
                listener.onClick(event);
            } catch (Throwable ignored) {
            }
        }
    }

    public void fireClickPost(GuiClickPostEvent event) {
        GuiListener[] snap = orderedClickPost;
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
        GuiDragListener[] drags = dragOnly;
        GuiListener[] guiDrags = orderedDrag;
        if (drags.length == 0 && guiDrags.length == 0) {
            return;
        }
        for (GuiDragListener listener : drags) {
            try {
                listener.onInventoryDrag(event);
            } catch (Throwable ignored) {
            }
        }
        for (GuiListener listener : guiDrags) {
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
            orderedClose = EMPTY;
            orderedClick = EMPTY;
            orderedClickPost = EMPTY;
            orderedDrag = EMPTY;
            return;
        }
        ArrayList<GuiListener> copy = new ArrayList<>(listeners);
        copy.sort(BY_PRIORITY);
        ordered = copy.toArray(EMPTY);
        orderedOpen = filterOverrides(copy, "onOpen", GuiOpenEvent.class);
        orderedClose = filterOverrides(copy, "onClose", GuiCloseEvent.class);
        orderedClick = filterOverrides(copy, "onClick", GuiClickEvent.class);
        orderedClickPost = filterOverrides(copy, "onClickPost", GuiClickPostEvent.class);
        orderedDrag = filterOverrides(copy, "onDrag", GuiDragEvent.class);
    }

    private void rebuildOpenOnly() {
        openOnly = openListeners.isEmpty() ? EMPTY_OPEN : openListeners.toArray(EMPTY_OPEN);
    }

    private void rebuildCloseOnly() {
        closeOnly = closeListeners.isEmpty() ? EMPTY_CLOSE : closeListeners.toArray(EMPTY_CLOSE);
    }

    private void rebuildClickOnly() {
        clickOnly = clickListeners.isEmpty() ? EMPTY_CLICK : clickListeners.toArray(EMPTY_CLICK);
    }

    private void rebuildDragOnly() {
        dragOnly = dragListeners.isEmpty() ? EMPTY_DRAG : dragListeners.toArray(EMPTY_DRAG);
    }

    private static GuiListener[] filterOverrides(
            ArrayList<GuiListener> copy,
            String method,
            Class<?> param
    ) {
        ArrayList<GuiListener> matched = new ArrayList<>(copy.size());
        for (GuiListener listener : copy) {
            if (overrides(listener, method, param)) {
                matched.add(listener);
            }
        }
        return matched.isEmpty() ? EMPTY : matched.toArray(EMPTY);
    }

    private static boolean overrides(GuiListener listener, String method, Class<?> param) {
        Class<?> type = listener.getClass();
        while (type != null && type != Object.class) {
            try {
                type.getDeclaredMethod(method, param);
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
