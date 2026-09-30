package org.vrz.relay.api.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Unified multi-threading scheduler abstraction across Paper and Folia architectures.
 * <p>
 * Eliminates platform-dependent scheduler complexity by transparently routing tasks
 * to Folia's Regional, Entity, and Global schedulers when running on Folia, or to
 * Paper's BukkitScheduler on standard servers.
 */
public interface SchedulerService {

    /**
     * Checks if the active server runtime is running on multi-threaded Folia.
     *
     * @return true if running on Folia
     */
    boolean isFolia();

    // ==========================================
    // Entity Scheduling
    // ==========================================

    /**
     * Executes a task on the thread context bound to the specified entity.
     *
     * @param plugin owning plugin
     * @param entity target entity
     * @param task   runnable task
     * @return task handle
     */
    @NotNull
    RelayTask runFor(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task);

    /**
     * Schedules a task to execute on the entity thread context after a delay.
     *
     * @param plugin     owning plugin
     * @param entity     target entity
     * @param task       runnable task
     * @param delayTicks delay in server ticks
     * @return task handle
     */
    @NotNull
    RelayTask runForLater(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task, long delayTicks);

    /**
     * Schedules a repeating task on the entity thread context.
     *
     * @param plugin      owning plugin
     * @param entity      target entity
     * @param task        runnable task
     * @param delayTicks  delay before first execution
     * @param periodTicks period between executions
     * @return task handle
     */
    @NotNull
    RelayTask runForRepeating(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task, long delayTicks, long periodTicks);

    /**
     * Schedules a repeating task on the entity thread context with access to the task handle.
     *
     * @param plugin      owning plugin
     * @param entity      target entity
     * @param task        consumer accepting the task handle
     * @param delayTicks  delay before first execution
     * @param periodTicks period between executions
     * @return task handle
     */
    @NotNull
    RelayTask runForRepeating(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Consumer<RelayTask> task, long delayTicks, long periodTicks);

    // ==========================================
    // Region Scheduling (Location)
    // ==========================================

    /**
     * Executes a task on the thread context owning the specified location.
     *
     * @param plugin   owning plugin
     * @param location target location
     * @param task     runnable task
     * @return task handle
     */
    @NotNull
    RelayTask runAt(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable task);

    /**
     * Schedules a task to execute on the location thread context after a delay.
     *
     * @param plugin     owning plugin
     * @param location   target location
     * @param task       runnable task
     * @param delayTicks delay in server ticks
     * @return task handle
     */
    @NotNull
    RelayTask runAtLater(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable task, long delayTicks);

    /**
     * Schedules a repeating task on the location thread context.
     *
     * @param plugin      owning plugin
     * @param location    target location
     * @param task        runnable task
     * @param delayTicks  delay before first execution
     * @param periodTicks period between executions
     * @return task handle
     */
    @NotNull
    RelayTask runAtRepeating(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable task, long delayTicks, long periodTicks);

    /**
     * Schedules a repeating task on the location thread context with access to the task handle.
     *
     * @param plugin      owning plugin
     * @param location    target location
     * @param task        consumer accepting the task handle
     * @param delayTicks  delay before first execution
     * @param periodTicks period between executions
     * @return task handle
     */
    @NotNull
    RelayTask runAtRepeating(@NotNull Plugin plugin, @NotNull Location location, @NotNull Consumer<RelayTask> task, long delayTicks, long periodTicks);

    // ==========================================
    // Global Region Scheduling
    // ==========================================

    /**
     * Executes a task on the server global region thread (main thread on Paper).
     *
     * @param plugin owning plugin
     * @param task   runnable task
     * @return task handle
     */
    @NotNull
    RelayTask runGlobal(@NotNull Plugin plugin, @NotNull Runnable task);

    /**
     * Schedules a task to execute on the global region thread after a delay.
     *
     * @param plugin     owning plugin
     * @param task       runnable task
     * @param delayTicks delay in server ticks
     * @return task handle
     */
    @NotNull
    RelayTask runGlobalLater(@NotNull Plugin plugin, @NotNull Runnable task, long delayTicks);

    /**
     * Schedules a repeating task on the global region thread.
     *
     * @param plugin      owning plugin
     * @param task        runnable task
     * @param delayTicks  delay before first execution
     * @param periodTicks period between executions
     * @return task handle
     */
    @NotNull
    RelayTask runGlobalRepeating(@NotNull Plugin plugin, @NotNull Runnable task, long delayTicks, long periodTicks);

    /**
     * Schedules a repeating task on the global region thread with access to the task handle.
     *
     * @param plugin      owning plugin
     * @param task        consumer accepting the task handle
     * @param delayTicks  delay before first execution
     * @param periodTicks period between executions
     * @return task handle
     */
    @NotNull
    RelayTask runGlobalRepeating(@NotNull Plugin plugin, @NotNull Consumer<RelayTask> task, long delayTicks, long periodTicks);

    // ==========================================
    // Asynchronous Worker Scheduling
    // ==========================================

    /**
     * Executes a task immediately on an asynchronous worker thread pool.
     *
     * @param plugin owning plugin
     * @param task   runnable task
     * @return task handle
     */
    @NotNull
    RelayTask runAsync(@NotNull Plugin plugin, @NotNull Runnable task);

    /**
     * Schedules a task to execute asynchronously after a specified time duration.
     *
     * @param plugin   owning plugin
     * @param task     runnable task
     * @param delay    delay value
     * @param timeUnit time unit
     * @return task handle
     */
    @NotNull
    RelayTask runAsyncLater(@NotNull Plugin plugin, @NotNull Runnable task, long delay, @NotNull TimeUnit timeUnit);

    /**
     * Schedules a repeating task on an asynchronous worker thread pool.
     *
     * @param plugin   owning plugin
     * @param task     runnable task
     * @param delay    initial delay
     * @param period   repeating interval
     * @param timeUnit time unit
     * @return task handle
     */
    @NotNull
    RelayTask runAsyncRepeating(@NotNull Plugin plugin, @NotNull Runnable task, long delay, long period, @NotNull TimeUnit timeUnit);

    /**
     * Executes a task asynchronously and returns a {@link CompletableFuture}.
     *
     * @param task runnable task
     * @return future completed when task finishes
     */
    @NotNull
    CompletableFuture<Void> runAsyncPromise(@NotNull Runnable task);

    /**
     * Executes a supplier asynchronously and returns a {@link CompletableFuture} with the result.
     *
     * @param supplier result supplier
     * @param <T>      result type
     * @return future containing the result
     */
    @NotNull
    <T> CompletableFuture<T> supplyAsyncPromise(@NotNull Supplier<T> supplier);

    // ==========================================
    // Thread Verification Checks
    // ==========================================

    /**
     * Checks if the currently executing thread owns the specified entity.
     *
     * @param entity target entity
     * @return true if currently on the entity's thread
     */
    boolean isEntityThread(@NotNull Entity entity);

    /**
     * Checks if the currently executing thread owns the specified location.
     *
     * @param location target location
     * @return true if currently on the location's region thread
     */
    boolean isRegionThread(@NotNull Location location);

    /**
     * Checks if the currently executing thread is the global region thread (or main thread).
     *
     * @return true if on global/main thread
     */
    boolean isGlobalThread();

    // ==========================================
    // Lifecycle & Diagnostics
    // ==========================================

    /**
     * Cancels all active tasks scheduled by the specified plugin.
     *
     * @param plugin owning plugin
     */
    void cancelAll(@NotNull Plugin plugin);

    /**
     * Gets the total number of currently active scheduled tasks.
     *
     * @return active task count
     */
    int getActiveTaskCount();

    /**
     * Gets the number of active tasks scheduled by a specific plugin.
     *
     * @param plugin owning plugin
     * @return active task count
     */
    int getActiveTaskCount(@NotNull Plugin plugin);
}
