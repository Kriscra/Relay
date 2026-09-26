package org.vrz.relay.data;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.data.DataChangeListener;
import org.vrz.relay.api.data.DataKey;
import org.vrz.relay.api.messenger.Subscription;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Subscription descriptor for data key mutation observers.
 *
 * @param <T> data type
 */
public final class DataObserverEntry<T> implements Subscription {

    private final DataKey<T> key;
    private final Plugin plugin;
    private final DataChangeListener<T> listener;
    private final Consumer<DataObserverEntry<T>> unregisterCallback;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public DataObserverEntry(@NotNull DataKey<T> key,
                             @NotNull Plugin plugin,
                             @NotNull DataChangeListener<T> listener,
                             @NotNull Consumer<DataObserverEntry<T>> unregisterCallback) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.listener = Objects.requireNonNull(listener, "listener cannot be null");
        this.unregisterCallback = Objects.requireNonNull(unregisterCallback, "unregisterCallback cannot be null");
    }

    @Override
    @NotNull
    public String getTopic() {
        return key.getFullKey();
    }

    @NotNull
    public DataKey<T> getKey() {
        return key;
    }

    @Override
    @NotNull
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public boolean isActive() {
        return active.get();
    }

    @Override
    public void unsubscribe() {
        if (active.compareAndSet(true, false)) {
            unregisterCallback.accept(this);
        }
    }

    public void dispatch(@Nullable UUID targetId, @Nullable T oldValue, @Nullable T newValue) {
        if (isActive()) {
            listener.onDataChange(targetId, key, oldValue, newValue);
        }
    }
}
