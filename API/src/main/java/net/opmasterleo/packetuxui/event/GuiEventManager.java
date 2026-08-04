package net.opmasterleo.packetuxui.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GuiEventManager {

    private static final Comparator<GuiListener> BY_PRIORITY =
            Comparator.comparingInt(l -> l.priority().ordinal());

    private final CopyOnWriteArrayList<GuiListener> listeners = new CopyOnWriteArrayList<>();

    public void register(GuiListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.addIfAbsent(listener);
    }

    public void unregister(GuiListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void clear() {
        listeners.clear();
    }

    public List<GuiListener> listeners() {
        return List.copyOf(listeners);
    }

    public void fireOpen(GuiOpenEvent event) {
        for (GuiListener listener : ordered()) {
            try {
                listener.onOpen(event);
            } catch (Throwable ignored) {
            }
        }
    }

    public void fireClose(GuiCloseEvent event) {
        for (GuiListener listener : ordered()) {
            try {
                listener.onClose(event);
            } catch (Throwable ignored) {
            }
        }
    }

    public void fireClick(GuiClickEvent event) {
        for (GuiListener listener : ordered()) {
            try {
                listener.onClick(event);
            } catch (Throwable ignored) {
            }
        }
    }

    public void fireClickPost(GuiClickPostEvent event) {
        List<GuiListener> ordered = ordered();
        for (int i = ordered.size() - 1; i >= 0; i--) {
            try {
                ordered.get(i).onClickPost(event);
            } catch (Throwable ignored) {
            }
        }
    }

    private List<GuiListener> ordered() {
        ArrayList<GuiListener> copy = new ArrayList<>(listeners);
        copy.sort(BY_PRIORITY);
        return copy;
    }
}
