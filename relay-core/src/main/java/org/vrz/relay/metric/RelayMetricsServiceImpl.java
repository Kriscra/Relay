package org.vrz.relay.metric;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.event.PerformanceAlertEvent;
import org.vrz.relay.api.metric.MetricScope;
import org.vrz.relay.api.metric.MetricSnapshot;
import org.vrz.relay.api.metric.MetricsService;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.DoubleSupplier;
import java.util.logging.Logger;

/**
 * Thread-safe, non-blocking implementation of {@link MetricsService}.
 */
public class RelayMetricsServiceImpl implements MetricsService {

    private static final int RESERVOIR_SIZE = 1024;
    private static final Logger LOGGER = Logger.getLogger("Relay-Metrics");

    private final ConcurrentHashMap<String, TimerTracker> timers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DoubleSupplier> dynamicGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Double> staticGauges = new ConcurrentHashMap<>();

    private final AtomicLong latencyAlertThresholdMillis = new AtomicLong(50L); // 50ms default = 1 tick

    @Override
    @NotNull
    public MetricScope startTimer(@NotNull String name) {
        Objects.requireNonNull(name, "metric name cannot be null");
        long startNanos = System.nanoTime();
        return new MetricScope() {
            private boolean closed = false;

            @Override
            public void close() {
                if (!closed) {
                    closed = true;
                    long elapsed = System.nanoTime() - startNanos;
                    recordTime(name, elapsed);
                }
            }

            @Override
            public long elapsedNanos() {
                return System.nanoTime() - startNanos;
            }
        };
    }

    @Override
    public void recordTime(@NotNull String name, long durationNanos) {
        Objects.requireNonNull(name, "metric name cannot be null");
        if (durationNanos < 0) durationNanos = 0;

        TimerTracker tracker = timers.computeIfAbsent(name, TimerTracker::new);
        tracker.record(durationNanos);

        long threshold = latencyAlertThresholdMillis.get();
        if (threshold > 0 && durationNanos >= threshold * 1_000_000L) {
            triggerAlert(name, durationNanos, threshold);
        }
    }

    private void triggerAlert(@NotNull String name, long durationNanos, long thresholdMillis) {
        boolean isPrimary = false;
        String threadName = Thread.currentThread().getName();

        try {
            if (Bukkit.getServer() != null) {
                isPrimary = Bukkit.isPrimaryThread();
                PerformanceAlertEvent event = new PerformanceAlertEvent(name, durationNanos, thresholdMillis, isPrimary, threadName);
                Bukkit.getPluginManager().callEvent(event);
            }
        } catch (Throwable ignored) {
            // Unit tests or early startup
        }

        if (isPrimary) {
            double durationMs = durationNanos / 1_000_000.0;
            LOGGER.warning(String.format("[LAG SPIKE] Operation '%s' took %.2fms on primary thread! (Threshold: %dms)",
                    name, durationMs, thresholdMillis));
        }
    }

    @Override
    public void increment(@NotNull String name, long delta) {
        Objects.requireNonNull(name, "counter name cannot be null");
        counters.computeIfAbsent(name, k -> new LongAdder()).add(delta);
    }

    @Override
    public long getCount(@NotNull String name) {
        LongAdder adder = counters.get(name);
        return adder != null ? adder.sum() : 0L;
    }

    @Override
    public void registerGauge(@NotNull String name, @NotNull DoubleSupplier supplier) {
        Objects.requireNonNull(name, "gauge name cannot be null");
        Objects.requireNonNull(supplier, "supplier cannot be null");
        dynamicGauges.put(name, supplier);
    }

    @Override
    public void setGauge(@NotNull String name, double value) {
        Objects.requireNonNull(name, "gauge name cannot be null");
        staticGauges.put(name, value);
    }

    @Override
    public double getGaugeValue(@NotNull String name) {
        DoubleSupplier dynamic = dynamicGauges.get(name);
        if (dynamic != null) {
            try {
                return dynamic.getAsDouble();
            } catch (Exception e) {
                return 0.0;
            }
        }
        Double val = staticGauges.get(name);
        return val != null ? val : 0.0;
    }

    @Override
    @NotNull
    public Optional<MetricSnapshot> getSnapshot(@NotNull String name) {
        TimerTracker tracker = timers.get(name);
        return tracker != null ? Optional.of(tracker.createSnapshot()) : Optional.empty();
    }

    @Override
    @NotNull
    public Map<String, MetricSnapshot> getAllSnapshots() {
        Map<String, MetricSnapshot> snapshots = new LinkedHashMap<>();
        for (Map.Entry<String, TimerTracker> entry : timers.entrySet()) {
            snapshots.put(entry.getKey(), entry.getValue().createSnapshot());
        }
        return Collections.unmodifiableMap(snapshots);
    }

    @Override
    @NotNull
    public Map<String, Double> getAllGauges() {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : staticGauges.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, DoubleSupplier> entry : dynamicGauges.entrySet()) {
            try {
                result.put(entry.getKey(), entry.getValue().getAsDouble());
            } catch (Exception ignored) {}
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    @NotNull
    public Map<String, Long> getAllCounters() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, LongAdder> entry : counters.entrySet()) {
            result.put(entry.getKey(), entry.getValue().sum());
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public void setLatencyAlertThreshold(long thresholdMillis) {
        this.latencyAlertThresholdMillis.set(thresholdMillis);
    }

    @Override
    public long getLatencyAlertThreshold() {
        return this.latencyAlertThresholdMillis.get();
    }

    @Override
    public void reset() {
        timers.clear();
        counters.clear();
        staticGauges.clear();
    }

    @Override
    public void reset(@NotNull String name) {
        timers.remove(name);
        counters.remove(name);
        staticGauges.remove(name);
        dynamicGauges.remove(name);
    }

    /**
     * Internal rolling statistics tracker for a single named timer.
     */
    private static final class TimerTracker {
        private final String name;
        private final LongAdder count = new LongAdder();
        private final LongAdder totalNanos = new LongAdder();
        private final AtomicLong minNanos = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxNanos = new AtomicLong(0L);
        private final AtomicLong lastExecutionNanos = new AtomicLong(0L);
        private final AtomicReference<Instant> lastRecordedAt = new AtomicReference<>(Instant.now());

        private final long[] reservoir = new long[RESERVOIR_SIZE];
        private final AtomicInteger reservoirIndex = new AtomicInteger(0);

        private TimerTracker(@NotNull String name) {
            this.name = name;
        }

        private void record(long durationNanos) {
            count.increment();
            totalNanos.add(durationNanos);
            minNanos.accumulateAndGet(durationNanos, Math::min);
            maxNanos.accumulateAndGet(durationNanos, Math::max);
            lastExecutionNanos.set(durationNanos);
            lastRecordedAt.set(Instant.now());

            int idx = Math.abs(reservoirIndex.getAndIncrement() % RESERVOIR_SIZE);
            reservoir[idx] = durationNanos;
        }

        @NotNull
        private MetricSnapshot createSnapshot() {
            long totalCount = count.sum();
            if (totalCount == 0) {
                return new MetricSnapshot(name, 0, 0, 0, 0, 0, 0, 0, 0, lastRecordedAt.get());
            }

            long totalTime = totalNanos.sum();
            double avgMillis = (totalTime / 1_000_000.0) / totalCount;
            long min = minNanos.get();
            double minMillis = (min == Long.MAX_VALUE) ? 0.0 : min / 1_000_000.0;
            double maxMillis = maxNanos.get() / 1_000_000.0;
            long lastNanos = lastExecutionNanos.get();

            // Calculate p95 and p99 from reservoir
            int samplesToTake = (int) Math.min(totalCount, RESERVOIR_SIZE);
            long[] sampleCopy = new long[samplesToTake];
            System.arraycopy(reservoir, 0, sampleCopy, 0, samplesToTake);
            Arrays.sort(sampleCopy);

            double p95Millis = 0.0;
            double p99Millis = 0.0;

            if (samplesToTake > 0) {
                int p95Idx = (int) Math.ceil(0.95 * samplesToTake) - 1;
                int p99Idx = (int) Math.ceil(0.99 * samplesToTake) - 1;
                p95Millis = sampleCopy[Math.max(0, Math.min(p95Idx, samplesToTake - 1))] / 1_000_000.0;
                p99Millis = sampleCopy[Math.max(0, Math.min(p99Idx, samplesToTake - 1))] / 1_000_000.0;
            }

            return new MetricSnapshot(
                    name,
                    totalCount,
                    totalTime,
                    minMillis,
                    maxMillis,
                    avgMillis,
                    p95Millis,
                    p99Millis,
                    lastNanos,
                    lastRecordedAt.get()
            );
        }
    }
}
