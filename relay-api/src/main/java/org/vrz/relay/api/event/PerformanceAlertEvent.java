package org.vrz.relay.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Dispatched when a monitored code operation or service call exceeds the configured
 * execution latency threshold, indicating a potential server lag spike.
 */
public class PerformanceAlertEvent extends RelayEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String metricName;
    private final long durationNanos;
    private final long thresholdMillis;
    private final boolean isPrimaryThread;
    private final String threadName;

    public PerformanceAlertEvent(@NotNull String metricName,
                                 long durationNanos,
                                 long thresholdMillis,
                                 boolean isPrimaryThread,
                                 @NotNull String threadName) {
        super(true); // Asynchronously fired to avoid blocking execution
        this.metricName = metricName;
        this.durationNanos = durationNanos;
        this.thresholdMillis = thresholdMillis;
        this.isPrimaryThread = isPrimaryThread;
        this.threadName = threadName;
    }

    @NotNull
    public String getMetricName() {
        return metricName;
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    public double getDurationMillis() {
        return durationNanos / 1_000_000.0;
    }

    public long getThresholdMillis() {
        return thresholdMillis;
    }

    public boolean isPrimaryThread() {
        return isPrimaryThread;
    }

    @NotNull
    public String getThreadName() {
        return threadName;
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
