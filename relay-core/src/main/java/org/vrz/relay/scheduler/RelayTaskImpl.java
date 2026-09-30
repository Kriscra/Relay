package org.vrz.relay.scheduler;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.scheduler.RelayTask;
import org.vrz.relay.api.scheduler.TaskExecutionType;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class RelayTaskImpl implements RelayTask {

    private final UUID taskId;
    private final Plugin plugin;
    private final TaskExecutionType executionType;
    private final boolean repeating;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final Runnable cancelHandler;
    private final Consumer<RelayTaskImpl> onCleanup;

    public RelayTaskImpl(@NotNull Plugin plugin,
                         @NotNull TaskExecutionType executionType,
                         boolean repeating,
                         @Nullable Runnable cancelHandler,
                         @Nullable Consumer<RelayTaskImpl> onCleanup) {
        this.taskId = UUID.randomUUID();
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.executionType = Objects.requireNonNull(executionType, "executionType cannot be null");
        this.repeating = repeating;
        this.cancelHandler = cancelHandler;
        this.onCleanup = onCleanup;
    }

    @Override
    @NotNull
    public UUID getTaskId() {
        return taskId;
    }

    @Override
    @NotNull
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    @NotNull
    public TaskExecutionType getExecutionType() {
        return executionType;
    }

    @Override
    public boolean isRepeating() {
        return repeating;
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            if (cancelHandler != null) {
                try {
                    cancelHandler.run();
                } catch (Throwable ignored) {}
            }
            if (onCleanup != null) {
                try {
                    onCleanup.accept(this);
                } catch (Throwable ignored) {}
            }
        }
    }

    /**
     * Internal method called when a non-repeating task completes naturally.
     */
    public void markCompleted() {
        if (!repeating) {
            cancelled.set(true);
            if (onCleanup != null) {
                try {
                    onCleanup.accept(this);
                } catch (Throwable ignored) {}
            }
        }
    }
}
