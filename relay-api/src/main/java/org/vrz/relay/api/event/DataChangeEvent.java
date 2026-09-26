package org.vrz.relay.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.data.DataKey;

import java.util.UUID;

/**
 * Dispatched when a shared data value is updated or removed in {@link org.vrz.relay.api.data.SharedDataService}.
 *
 * @param <T> data type
 */
public class DataChangeEvent<T> extends RelayEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID targetId;
    private final DataKey<T> key;
    private final T oldValue;
    private final T newValue;

    public DataChangeEvent(@Nullable UUID targetId,
                           @NotNull DataKey<T> key,
                           @Nullable T oldValue,
                           @Nullable T newValue) {
        super(true);
        this.targetId = targetId;
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Nullable
    public UUID getTargetId() {
        return targetId;
    }

    public boolean isGlobal() {
        return targetId == null;
    }

    @NotNull
    public DataKey<T> getKey() {
        return key;
    }

    @Nullable
    public T getOldValue() {
        return oldValue;
    }

    @Nullable
    public T getNewValue() {
        return newValue;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
