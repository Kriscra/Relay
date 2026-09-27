package org.vrz.relay.api.metric;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * High-performance, lock-free telemetry and performance profiling service for Relay.
 * <p>
 * Allows plugins to measure execution timings, monitor call frequencies, record counters,
 * and track gauges with nanosecond precision and minimal overhead.
 */
public interface MetricsService {

    /**
     * Starts a new timing scope for the specified metric key.
     * When closed via {@link MetricScope#close()} or try-with-resources, the elapsed duration is recorded.
     *
     * @param name unique name of the metric (e.g. "clan:bank_deposit", "spell:cast")
     * @return an AutoCloseable metric scope
     */
    @NotNull
    MetricScope startTimer(@NotNull String name);

    /**
     * Manually records an elapsed duration for a metric.
     *
     * @param name          unique name of the metric
     * @param durationNanos execution duration in nanoseconds
     */
    void recordTime(@NotNull String name, long durationNanos);

    /**
     * Profiles the execution of a {@link Runnable} task and records its elapsed duration.
     *
     * @param name     metric name
     * @param runnable task to profile
     */
    default void profile(@NotNull String name, @NotNull Runnable runnable) {
        try (MetricScope ignored = startTimer(name)) {
            runnable.run();
        }
    }

    /**
     * Profiles the execution of a {@link Supplier} task, returning its result while recording duration.
     *
     * @param name     metric name
     * @param supplier task to profile
     * @param <T>      return type
     * @return the result of the supplier
     */
    default <T> T profileSupplier(@NotNull String name, @NotNull Supplier<T> supplier) {
        try (MetricScope ignored = startTimer(name)) {
            return supplier.get();
        }
    }

    /**
     * Atomically increments a counter by 1.
     *
     * @param name counter name
     */
    default void increment(@NotNull String name) {
        increment(name, 1L);
    }

    /**
     * Atomically increments a counter by a given delta.
     *
     * @param name  counter name
     * @param delta amount to increment
     */
    void increment(@NotNull String name, long delta);

    /**
     * Gets the current value of a counter.
     *
     * @param name counter name
     * @return current count, or 0 if not present
     */
    long getCount(@NotNull String name);

    /**
     * Registers a dynamic gauge whose value is computed on-demand via a {@link DoubleSupplier}.
     *
     * @param name     gauge name (e.g. "cache:active_sessions")
     * @param supplier function providing the current value
     */
    void registerGauge(@NotNull String name, @NotNull DoubleSupplier supplier);

    /**
     * Sets a static gauge value.
     *
     * @param name  gauge name
     * @param value current numeric value
     */
    void setGauge(@NotNull String name, double value);

    /**
     * Gets the current value of a gauge.
     *
     * @param name gauge name
     * @return current value, or 0.0 if not registered
     */
    double getGaugeValue(@NotNull String name);

    /**
     * Retrieves an immutable statistical snapshot for a timing metric.
     *
     * @param name metric name
     * @return optional containing the snapshot if any samples have been recorded
     */
    @NotNull
    Optional<MetricSnapshot> getSnapshot(@NotNull String name);

    /**
     * Returns all recorded timer snapshots.
     *
     * @return unmodifiable map of metric names to snapshots
     */
    @NotNull
    Map<String, MetricSnapshot> getAllSnapshots();

    /**
     * Returns all registered gauge values at this moment.
     *
     * @return unmodifiable map of gauge names to values
     */
    @NotNull
    Map<String, Double> getAllGauges();

    /**
     * Returns all registered counter values.
     *
     * @return unmodifiable map of counter names to counts
     */
    @NotNull
    Map<String, Long> getAllCounters();

    /**
     * Sets the main-thread latency alert threshold in milliseconds.
     * If an operation on the primary server thread exceeds this duration, a warning is logged
     * and a {@link org.vrz.relay.api.event.PerformanceAlertEvent} is dispatched.
     *
     * @param thresholdMillis threshold in ms (e.g. 50ms = 1 full server tick)
     */
    void setLatencyAlertThreshold(long thresholdMillis);

    /**
     * Gets the current latency alert threshold in milliseconds.
     *
     * @return threshold in ms
     */
    long getLatencyAlertThreshold();

    /**
     * Resets all recorded metrics (timers, counters, static gauges).
     */
    void reset();

    /**
     * Resets a specific metric by name.
     *
     * @param name metric name to reset
     */
    void reset(@NotNull String name);
}
