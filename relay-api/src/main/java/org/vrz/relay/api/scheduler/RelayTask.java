package org.vrz.relay.api.scheduler;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Handle to an actively running or scheduled task within the Relay scheduler system.
 * Provides thread-safe cancellation across Paper, Purpur, and Folia platforms.
 */
public interface RelayTask {

    /**
     * Gets the unique identifier of this task.
     *
     * @return unique task id
     */
    @NotNull
    UUID getTaskId();

    /**
     * Gets the plugin that scheduled this task.
     *
     * @return owning plugin
     */
    @NotNull
    Plugin getPlugin();

    /**
     * Gets the execution context model of this task.
     *
     * @return execution type
     */
    @NotNull
    TaskExecutionType getExecutionType();

    /**
     * Checks if this task repeats periodically.
     *
     * @return true if repeating
     */
    boolean isRepeating();

    /**
     * Checks if this task has been cancelled or completed.
     *
     * @return true if cancelled or completed
     */
    boolean isCancelled();

    /**
     * Cancels this task, removing it from execution queues.
     */
    void cancel();
}
