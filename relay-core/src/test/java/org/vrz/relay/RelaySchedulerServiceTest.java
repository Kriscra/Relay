package org.vrz.relay;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.vrz.relay.api.scheduler.RelayTask;
import org.vrz.relay.api.scheduler.TaskExecutionType;
import org.vrz.relay.scheduler.RelaySchedulerServiceImpl;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RelaySchedulerServiceTest {

    private RelaySchedulerServiceImpl scheduler;
    private Plugin pluginA;
    private Plugin pluginB;
    private Player mockPlayer;
    private Location mockLocation;

    @BeforeEach
    void setUp() {
        scheduler = new RelaySchedulerServiceImpl();

        pluginA = Mockito.mock(Plugin.class);
        Mockito.when(pluginA.getName()).thenReturn("PluginA");

        pluginB = Mockito.mock(Plugin.class);
        Mockito.when(pluginB.getName()).thenReturn("PluginB");

        mockPlayer = Mockito.mock(Player.class);
        Mockito.when(mockPlayer.getUniqueId()).thenReturn(UUID.randomUUID());
        Mockito.when(mockPlayer.isValid()).thenReturn(true);

        World mockWorld = Mockito.mock(World.class);
        Mockito.when(mockWorld.getName()).thenReturn("world");
        mockLocation = new Location(mockWorld, 100, 64, 100);
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdown();
    }

    @Test
    @DisplayName("Async promise execution and supplyAsyncPromise")
    void testAsyncPromises() throws Exception {
        CompletableFuture<String> future = scheduler.supplyAsyncPromise(() -> "Relay Scheduler Fast");
        String result = future.get(2, TimeUnit.SECONDS);
        assertEquals("Relay Scheduler Fast", result);

        AtomicBoolean ran = new AtomicBoolean(false);
        CompletableFuture<Void> runFuture = scheduler.runAsyncPromise(() -> ran.set(true));
        runFuture.get(2, TimeUnit.SECONDS);
        assertTrue(ran.get());
    }

    @Test
    @DisplayName("RunAt location schedules task and updates active task count")
    void testRunAtLocation() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        RelayTask task = scheduler.runAt(pluginA, mockLocation, latch::countDown);

        assertNotNull(task);
        assertEquals(TaskExecutionType.REGION, task.getExecutionType());
        assertFalse(task.isRepeating());

        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed, "Task at location should have executed");
    }

    @Test
    @DisplayName("Entity repeating task executes periodically and can be cancelled")
    void testRunForEntityRepeating() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        RelayTask task = scheduler.runForRepeating(pluginA, mockPlayer, t -> {
            int current = count.incrementAndGet();
            latch.countDown();
            if (current >= 3) {
                t.cancel();
            }
        }, 1L, 1L);

        assertNotNull(task);
        assertEquals(TaskExecutionType.ENTITY, task.getExecutionType());
        assertTrue(task.isRepeating());

        boolean finished = latch.await(3, TimeUnit.SECONDS);
        assertTrue(finished, "Repeating task should have ticked at least 3 times");
        assertTrue(task.isCancelled(), "Task should be marked cancelled after self-cancellation");
    }

    @Test
    @DisplayName("Zero-leak: cancelAll cancels all tasks for specific plugin without affecting others")
    void testCancelAllPluginTasks() {
        // Plugin A registers 3 tasks
        RelayTask taskA1 = scheduler.runGlobalRepeating(pluginA, () -> {}, 10L, 10L);
        RelayTask taskA2 = scheduler.runAtRepeating(pluginA, mockLocation, () -> {}, 10L, 10L);
        RelayTask taskA3 = scheduler.runForRepeating(pluginA, mockPlayer, () -> {}, 10L, 10L);

        // Plugin B registers 2 tasks
        RelayTask taskB1 = scheduler.runGlobalRepeating(pluginB, () -> {}, 10L, 10L);
        RelayTask taskB2 = scheduler.runAsyncRepeating(pluginB, () -> {}, 100L, 100L, TimeUnit.MILLISECONDS);

        assertEquals(3, scheduler.getActiveTaskCount(pluginA));
        assertEquals(2, scheduler.getActiveTaskCount(pluginB));
        assertEquals(5, scheduler.getActiveTaskCount());

        // Cancel all tasks for Plugin A
        scheduler.cancelAll(pluginA);

        assertTrue(taskA1.isCancelled());
        assertTrue(taskA2.isCancelled());
        assertTrue(taskA3.isCancelled());

        assertFalse(taskB1.isCancelled());
        assertFalse(taskB2.isCancelled());

        assertEquals(0, scheduler.getActiveTaskCount(pluginA));
        assertEquals(2, scheduler.getActiveTaskCount(pluginB));
        assertEquals(2, scheduler.getActiveTaskCount());
    }

    @Test
    @DisplayName("Task cancellation cleans up from active task registry")
    void testTaskManualCancellation() {
        RelayTask task = scheduler.runGlobalRepeating(pluginA, () -> {}, 100L, 100L);
        assertEquals(1, scheduler.getActiveTaskCount(pluginA));

        task.cancel();
        assertTrue(task.isCancelled());
        assertEquals(0, scheduler.getActiveTaskCount(pluginA));
    }
}
