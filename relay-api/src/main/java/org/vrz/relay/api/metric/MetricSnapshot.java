package org.vrz.relay.api.metric;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * Immutable aggregated snapshot of a measured execution timer metric.
 *
 * @param name               unique identifier of the metric
 * @param count              total number of recorded executions
 * @param totalNanos         total cumulative execution time in nanoseconds
 * @param minMillis          minimum recorded execution duration in milliseconds
 * @param maxMillis          maximum recorded execution duration in milliseconds
 * @param averageMillis      arithmetic mean execution duration in milliseconds
 * @param p95Millis          95th percentile execution duration in milliseconds
 * @param p99Millis          99th percentile execution duration in milliseconds
 * @param lastExecutionNanos duration of the most recent execution in nanoseconds
 * @param lastRecordedAt     timestamp of the most recent execution
 */
public record MetricSnapshot(
        @NotNull String name,
        long count,
        long totalNanos,
        double minMillis,
        double maxMillis,
        double averageMillis,
        double p95Millis,
        double p99Millis,
        long lastExecutionNanos,
        @NotNull Instant lastRecordedAt
) {
    /**
     * Gets the last execution duration in fractional milliseconds.
     *
     * @return last execution duration in ms
     */
    public double lastExecutionMillis() {
        return lastExecutionNanos / 1_000_000.0;
    }
}
