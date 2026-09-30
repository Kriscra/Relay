package org.vrz.relay.api.scheduler;

/**
 * Execution thread model categories for scheduled tasks across Paper and Folia architectures.
 */
public enum TaskExecutionType {

    /**
     * Executes on the server global region thread (or main thread on Paper).
     * Used for server-wide state, console commands, and world management.
     */
    GLOBAL,

    /**
     * Executes on the specific regional thread owning the target location/chunk.
     * On standard Paper, maps to the main thread.
     */
    REGION,

    /**
     * Executes on the thread context bound to a specific entity (e.g. player, mob).
     * Automatically cancelled if the entity is retired or invalid.
     */
    ENTITY,

    /**
     * Executes asynchronously off the server tick loop on a worker thread pool.
     */
    ASYNC
}
