package org.vrz.relay;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.vrz.relay.api.messenger.RequestTimeoutException;
import org.vrz.relay.api.messenger.Subscription;
import org.vrz.relay.messenger.RelayMessengerImpl;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RelayMessengerTest {

    private RelayMessengerImpl messenger;
    private Plugin dummyPlugin;

    @BeforeEach
    void setUp() {
        messenger = new RelayMessengerImpl();
        dummyPlugin = Mockito.mock(Plugin.class);
        Mockito.when(dummyPlugin.getName()).thenReturn("TestPlugin");
    }

    @AfterEach
    void tearDown() {
        messenger.shutdown();
    }

    @Test
    @DisplayName("Pub/Sub delivers payload asynchronously to subscribers")
    void testPubSubDelivery() throws Exception {
        CompletableFuture<String> receivedFuture = new CompletableFuture<>();

        Subscription sub = messenger.subscribe("test:channel", String.class, dummyPlugin, ctx -> {
            receivedFuture.complete(ctx.getPayload());
        });

        assertTrue(sub.isActive());
        assertEquals(1, messenger.getSubscriberCount("test:channel"));

        CompletableFuture<Integer> pubFuture = messenger.publish("test:channel", "Hello World!");
        Integer dispatched = pubFuture.get(2, TimeUnit.SECONDS);
        assertEquals(1, dispatched);

        String result = receivedFuture.get(2, TimeUnit.SECONDS);
        assertEquals("Hello World!", result);

        sub.unsubscribe();
        assertFalse(sub.isActive());
        assertEquals(0, messenger.getSubscriberCount("test:channel"));
    }

    @Test
    @DisplayName("RPC Request-Response succeeds with matching handler")
    void testSuccessfulRpcRequest() throws Exception {
        UUID queryId = UUID.randomUUID();

        // Register RPC responder
        messenger.handleRequestSync("clan:get_name", UUID.class, dummyPlugin, (ctx, id) -> {
            return "Vanguard";
        });

        assertTrue(messenger.hasRequestHandler("clan:get_name"));

        // Send RPC request
        CompletableFuture<String> future = messenger.request(
                "clan:get_name", queryId, String.class, Duration.ofSeconds(2)
        );

        String response = future.get(2, TimeUnit.SECONDS);
        assertEquals("Vanguard", response);
    }

    @Test
    @DisplayName("RPC Request times out when responder does not complete")
    void testRpcTimeout() {
        messenger.handleRequest("slow:topic", String.class, dummyPlugin, (ctx, req) -> {
            return new CompletableFuture<>(); // Never completes
        });

        CompletableFuture<String> future = messenger.request(
                "slow:topic", "ping", String.class, Duration.ofMillis(80)
        );

        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            future.get(1, TimeUnit.SECONDS);
        });

        assertInstanceOf(RequestTimeoutException.class, exception.getCause());
    }

    @Test
    @DisplayName("RPC Request fails when no handler is registered")
    void testRpcNoHandler() {
        CompletableFuture<String> future = messenger.request(
                "nonexistent:topic", "query", String.class, Duration.ofSeconds(1)
        );

        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            future.get(1, TimeUnit.SECONDS);
        });

        assertInstanceOf(NoSuchElementException.class, exception.getCause());
    }
}
