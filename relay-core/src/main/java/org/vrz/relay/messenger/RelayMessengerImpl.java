package org.vrz.relay.messenger;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.messenger.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Concrete high-throughput implementation of {@link RelayMessenger}
 * supporting both Pub/Sub and Request-Response (RPC) patterns.
 */
public class RelayMessengerImpl implements RelayMessenger {

    private static final Logger LOGGER = Logger.getLogger("RelayMessenger");

    private final Map<String, List<RelaySubscriptionImpl<?>>> topicSubscribers = new ConcurrentHashMap<>();
    private final Map<String, RelayRequestHandlerEntry<?, ?>> requestHandlers = new ConcurrentHashMap<>();
    private final ExecutorService asyncWorkerPool;
    private final ScheduledExecutorService timeoutScheduler;

    public RelayMessengerImpl() {
        this.asyncWorkerPool = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                new ThreadFactory() {
                    private final AtomicInteger count = new AtomicInteger(1);
                    @Override
                    public Thread newThread(@NotNull Runnable r) {
                        Thread t = new Thread(r, "Relay-MessengerWorker-" + count.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                }
        );

        this.timeoutScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Relay-RPCTimeoutScheduler");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    @NotNull
    public CompletableFuture<Integer> publish(@NotNull String topic,
                                             @NotNull Object payload,
                                             @Nullable Plugin publisher) {
        Objects.requireNonNull(topic, "topic cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");

        String normalizedTopic = topic.toLowerCase(Locale.ROOT);
        List<RelaySubscriptionImpl<?>> subs = topicSubscribers.get(normalizedTopic);
        if (subs == null || subs.isEmpty()) {
            return CompletableFuture.completedFuture(0);
        }

        RelayMessageContextImpl<?> context = new RelayMessageContextImpl<>(
                normalizedTopic, payload, Instant.now(), publisher
        );

        List<CompletableFuture<Void>> deliveryFutures = new ArrayList<>();
        AtomicInteger deliveredCount = new AtomicInteger(0);

        for (RelaySubscriptionImpl<?> sub : subs) {
            if (!sub.isActive() || !sub.matchesPayload(payload)) {
                continue;
            }

            deliveredCount.incrementAndGet();
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    sub.dispatch(context);
                } catch (Throwable t) {
                    LOGGER.log(Level.SEVERE, "Exception in message subscriber [" +
                            sub.getPlugin().getName() + "] for topic: " + normalizedTopic, t);
                }
            }, sub.getExecutor());

            deliveryFutures.add(future);
        }

        return CompletableFuture.allOf(deliveryFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> deliveredCount.get());
    }

    @Override
    @NotNull
    public <M> Subscription subscribe(@NotNull String topic,
                                     @NotNull Class<M> payloadClass,
                                     @NotNull Plugin subscriber,
                                     @NotNull MessageListener<M> listener) {
        return subscribe(topic, payloadClass, subscriber, asyncWorkerPool, listener);
    }

    @Override
    @NotNull
    public <M> Subscription subscribe(@NotNull String topic,
                                     @NotNull Class<M> payloadClass,
                                     @NotNull Plugin subscriber,
                                     @NotNull Executor executor,
                                     @NotNull MessageListener<M> listener) {
        Objects.requireNonNull(topic, "topic cannot be null");
        Objects.requireNonNull(payloadClass, "payloadClass cannot be null");
        Objects.requireNonNull(subscriber, "subscriber plugin cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");

        String normalizedTopic = topic.toLowerCase(Locale.ROOT);

        RelaySubscriptionImpl<M> sub = new RelaySubscriptionImpl<>(
                normalizedTopic,
                payloadClass,
                subscriber,
                executor,
                listener,
                this::removeSubscription
        );

        topicSubscribers.computeIfAbsent(normalizedTopic, k -> new CopyOnWriteArrayList<>()).add(sub);
        return sub;
    }

    private void removeSubscription(@NotNull RelaySubscriptionImpl<?> sub) {
        String topic = sub.getTopic();
        List<RelaySubscriptionImpl<?>> list = topicSubscribers.get(topic);
        if (list != null) {
            list.remove(sub);
            if (list.isEmpty()) {
                topicSubscribers.remove(topic);
            }
        }
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <REQ, RES> CompletableFuture<RES> request(@NotNull String topic,
                                                     @NotNull REQ requestPayload,
                                                     @NotNull Class<RES> responseClass,
                                                     @NotNull Duration timeout,
                                                     @Nullable Plugin requester) {
        Objects.requireNonNull(topic, "topic cannot be null");
        Objects.requireNonNull(requestPayload, "requestPayload cannot be null");
        Objects.requireNonNull(responseClass, "responseClass cannot be null");
        Objects.requireNonNull(timeout, "timeout cannot be null");

        String normalizedTopic = topic.toLowerCase(Locale.ROOT);
        RelayRequestHandlerEntry<?, ?> entry = requestHandlers.get(normalizedTopic);
        if (entry == null || !entry.isActive()) {
            return CompletableFuture.failedFuture(new NoSuchElementException(
                    "No active RPC request handler registered for topic: " + normalizedTopic
            ));
        }

        if (!entry.matchesRequest(requestPayload)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "Request payload of type " + requestPayload.getClass().getName() +
                    " does not match registered handler expectation: " + entry.getRequestClass().getName()
            ));
        }

        RelayMessageContextImpl<REQ> context = new RelayMessageContextImpl<>(
                normalizedTopic, requestPayload, Instant.now(), requester
        );

        CompletableFuture<RES> resultFuture = new CompletableFuture<>();

        ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
            if (!resultFuture.isDone()) {
                resultFuture.completeExceptionally(new RequestTimeoutException(normalizedTopic, timeout));
            }
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);

        entry.dispatch(context, requestPayload).whenComplete((rawResponse, throwable) -> {
            timeoutTask.cancel(false);
            if (throwable != null) {
                resultFuture.completeExceptionally(throwable);
            } else if (rawResponse == null) {
                resultFuture.completeExceptionally(new NullPointerException("RPC response was null"));
            } else if (!responseClass.isInstance(rawResponse)) {
                resultFuture.completeExceptionally(new ClassCastException(
                        "Expected response of type " + responseClass.getName() +
                        " but received " + rawResponse.getClass().getName()
                ));
            } else {
                resultFuture.complete(responseClass.cast(rawResponse));
            }
        });

        return resultFuture;
    }

    @Override
    @NotNull
    public <REQ, RES> Subscription handleRequest(@NotNull String topic,
                                                 @NotNull Class<REQ> requestClass,
                                                 @NotNull Plugin responder,
                                                 @NotNull RequestHandler<REQ, RES> handler) {
        return handleRequest(topic, requestClass, responder, asyncWorkerPool, handler);
    }

    @Override
    @NotNull
    public <REQ, RES> Subscription handleRequest(@NotNull String topic,
                                                 @NotNull Class<REQ> requestClass,
                                                 @NotNull Plugin responder,
                                                 @NotNull Executor executor,
                                                 @NotNull RequestHandler<REQ, RES> handler) {
        Objects.requireNonNull(topic, "topic cannot be null");
        Objects.requireNonNull(requestClass, "requestClass cannot be null");
        Objects.requireNonNull(responder, "responder plugin cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");

        String normalizedTopic = topic.toLowerCase(Locale.ROOT);

        RelayRequestHandlerEntry<REQ, RES> entry = new RelayRequestHandlerEntry<>(
                normalizedTopic,
                requestClass,
                responder,
                executor,
                handler,
                this::removeRequestHandler
        );

        requestHandlers.put(normalizedTopic, entry);
        return entry;
    }

    private void removeRequestHandler(@NotNull RelayRequestHandlerEntry<?, ?> entry) {
        requestHandlers.remove(entry.getTopic(), entry);
    }

    @Override
    public boolean hasRequestHandler(@NotNull String topic) {
        RelayRequestHandlerEntry<?, ?> entry = requestHandlers.get(topic.toLowerCase(Locale.ROOT));
        return entry != null && entry.isActive();
    }

    @Override
    public void unsubscribeAll(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");

        // Clean topic subscribers
        for (List<RelaySubscriptionImpl<?>> subs : topicSubscribers.values()) {
            for (RelaySubscriptionImpl<?> sub : subs) {
                if (sub.getPlugin().equals(plugin)) {
                    sub.unsubscribe();
                }
            }
        }

        // Clean request handlers
        for (RelayRequestHandlerEntry<?, ?> entry : requestHandlers.values()) {
            if (entry.getPlugin().equals(plugin)) {
                entry.unsubscribe();
            }
        }
    }

    @Override
    @NotNull
    public Set<String> getActiveTopics() {
        Set<String> all = new HashSet<>(topicSubscribers.keySet());
        all.addAll(requestHandlers.keySet());
        return Collections.unmodifiableSet(all);
    }

    @Override
    public int getSubscriberCount(@NotNull String topic) {
        List<RelaySubscriptionImpl<?>> subs = topicSubscribers.get(topic.toLowerCase(Locale.ROOT));
        if (subs == null) return 0;
        int count = 0;
        for (RelaySubscriptionImpl<?> sub : subs) {
            if (sub.isActive()) count++;
        }
        return count;
    }

    public void shutdown() {
        timeoutScheduler.shutdownNow();
        asyncWorkerPool.shutdown();
        try {
            if (!asyncWorkerPool.awaitTermination(2, TimeUnit.SECONDS)) {
                asyncWorkerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncWorkerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        topicSubscribers.clear();
        requestHandlers.clear();
    }
}
