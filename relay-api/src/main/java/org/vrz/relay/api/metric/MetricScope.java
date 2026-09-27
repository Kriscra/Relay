package org.vrz.relay.api.metric;

/**
 * AutoCloseable timing scope designed for clean Java {@code try-with-resources} profiling.
 * <p>
 * Example:
 * <pre>{@code
 * try (MetricScope scope = RelayAPI.getMetrics().startTimer("combat:damage_eval")) {
 *     // Run expensive combat damage calculation
 * }
 * }</pre>
 */
public interface MetricScope extends AutoCloseable {

    /**
     * Stops the timer and records the elapsed duration in the metrics registry.
     */
    @Override
    void close();

    /**
     * Gets the elapsed time since this scope was started in nanoseconds.
     *
     * @return elapsed nanoseconds
     */
    long elapsedNanos();

    /**
     * Gets the elapsed time since this scope was started in milliseconds.
     *
     * @return elapsed milliseconds (fractional)
     */
    default double elapsedMillis() {
        return elapsedNanos() / 1_000_000.0;
    }
}
