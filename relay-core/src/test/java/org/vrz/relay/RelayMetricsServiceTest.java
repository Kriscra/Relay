package org.vrz.relay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.vrz.relay.api.metric.MetricScope;
import org.vrz.relay.api.metric.MetricSnapshot;
import org.vrz.relay.metric.RelayMetricsServiceImpl;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RelayMetricsServiceTest {

    private RelayMetricsServiceImpl metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new RelayMetricsServiceImpl();
    }

    @Test
    @DisplayName("Counters increment and aggregate correctly")
    void testCounters() {
        assertEquals(0, metricsService.getCount("packets:received"));

        metricsService.increment("packets:received");
        assertEquals(1, metricsService.getCount("packets:received"));

        metricsService.increment("packets:received", 5);
        assertEquals(6, metricsService.getCount("packets:received"));

        Map<String, Long> counters = metricsService.getAllCounters();
        assertTrue(counters.containsKey("packets:received"));
        assertEquals(6L, counters.get("packets:received"));
    }

    @Test
    @DisplayName("Static and dynamic gauges compute values properly")
    void testGauges() {
        metricsService.setGauge("server:tps", 19.98);
        assertEquals(19.98, metricsService.getGaugeValue("server:tps"), 0.001);

        AtomicInteger activePlayers = new AtomicInteger(42);
        metricsService.registerGauge("players:online", activePlayers::doubleValue);
        assertEquals(42.0, metricsService.getGaugeValue("players:online"), 0.001);

        activePlayers.set(75);
        assertEquals(75.0, metricsService.getGaugeValue("players:online"), 0.001);

        Map<String, Double> allGauges = metricsService.getAllGauges();
        assertEquals(19.98, allGauges.get("server:tps"), 0.001);
        assertEquals(75.0, allGauges.get("players:online"), 0.001);
    }

    @Test
    @DisplayName("Timer scope correctly records execution and produces statistical snapshots")
    void testTimerScope() throws Exception {
        String metric = "combat:damage_eval";

        try (MetricScope scope = metricsService.startTimer(metric)) {
            Thread.sleep(5);
            assertTrue(scope.elapsedNanos() > 0);
            assertTrue(scope.elapsedMillis() > 0);
        }

        Optional<MetricSnapshot> snapshotOpt = metricsService.getSnapshot(metric);
        assertTrue(snapshotOpt.isPresent());

        MetricSnapshot snapshot = snapshotOpt.get();
        assertEquals(metric, snapshot.name());
        assertEquals(1, snapshot.count());
        assertTrue(snapshot.minMillis() > 0);
        assertTrue(snapshot.maxMillis() > 0);
        assertTrue(snapshot.averageMillis() > 0);
        assertTrue(snapshot.lastExecutionMillis() > 0);
    }

    @Test
    @DisplayName("Profile runnable and supplier record execution times and return results")
    void testProfileRunnableAndSupplier() {
        String runKey = "task:cleanup";
        metricsService.profile(runKey, () -> {
            int sum = 0;
            for (int i = 0; i < 100; i++) sum += i;
        });

        assertEquals(1, metricsService.getSnapshot(runKey).map(MetricSnapshot::count).orElse(0L));

        String supKey = "task:computation";
        String result = metricsService.profileSupplier(supKey, () -> "success");
        assertEquals("success", result);
        assertEquals(1, metricsService.getSnapshot(supKey).map(MetricSnapshot::count).orElse(0L));
    }

    @Test
    @DisplayName("Percentiles p95 and p99 compute accurately across multiple samples")
    void testPercentiles() {
        String key = "bench:query";
        for (int i = 1; i <= 100; i++) {
            // record 1ms to 100ms in nanoseconds
            metricsService.recordTime(key, i * 1_000_000L);
        }

        MetricSnapshot snapshot = metricsService.getSnapshot(key).orElseThrow();
        assertEquals(100, snapshot.count());
        assertEquals(1.0, snapshot.minMillis(), 0.01);
        assertEquals(100.0, snapshot.maxMillis(), 0.01);
        assertEquals(50.5, snapshot.averageMillis(), 0.01);
        assertEquals(95.0, snapshot.p95Millis(), 0.1);
        assertEquals(99.0, snapshot.p99Millis(), 0.1);
    }

    @Test
    @DisplayName("Reset clears all or specific metrics")
    void testReset() {
        metricsService.increment("cnt1");
        metricsService.increment("cnt2");
        metricsService.setGauge("g1", 10.0);
        metricsService.recordTime("t1", 100_000L);

        metricsService.reset("cnt1");
        assertEquals(0, metricsService.getCount("cnt1"));
        assertEquals(1, metricsService.getCount("cnt2"));

        metricsService.reset();
        assertEquals(0, metricsService.getCount("cnt2"));
        assertEquals(0, metricsService.getAllSnapshots().size());
        assertEquals(0, metricsService.getAllGauges().size());
    }

    @Test
    @DisplayName("Latency alert threshold getter and setter work as expected")
    void testLatencyThreshold() {
        assertEquals(50L, metricsService.getLatencyAlertThreshold());
        metricsService.setLatencyAlertThreshold(25L);
        assertEquals(25L, metricsService.getLatencyAlertThreshold());
    }
}
