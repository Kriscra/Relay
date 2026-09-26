package org.vrz.relay.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Listener interface for observing mutations on shared data keys.
 *
 * @param <T> data type
 */
@FunctionalInterface
public interface DataChangeListener<T> {

    /**
     * Invoked when a data key value is set, updated, or removed.
     *
     * @param targetId player UUID or null if global
     * @param key      the data key that changed
     * @param oldValue the previous value, or null if newly set
     * @param newValue the new value, or null if removed/expired
     */
    void onDataChange(@Nullable UUID targetId,
                      @NotNull DataKey<T> key,
                      @Nullable T oldValue,
                      @Nullable T newValue);
}
