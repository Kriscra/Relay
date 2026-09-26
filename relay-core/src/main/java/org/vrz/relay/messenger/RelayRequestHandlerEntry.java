package org.vrz.relay.messenger;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.messenger.MessageContext;
import org.vrz.relay.api.messenger.RequestHandler;
import org.vrz.relay.api.messenger.Subscription;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Internal descriptor for an active RPC request handler.
 *
 * @param <REQ> request type
 * @param <RES> response type
 */
public final class RelayRequestHandlerEntry<REQ, RES> implements Subscription {

    private final String topic;
    private final Class<REQ> requestClass;
    private final Plugin plugin;
    private final Executor executor;
    private final RequestHandler<REQ, RES> handler;
    private final Consumer<RelayRequestHandlerEntry<REQ, RES>> unregisterCallback;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RelayRequestHandlerEntry(@NotNull String topic,
                                    @NotNull Class<REQ> requestClass,
                                    @NotNull Plugin plugin,
                                    @NotNull Executor executor,
                                    @NotNull RequestHandler<REQ, RES> handler,
                                    @NotNull Consumer<RelayRequestHandlerEntry<REQ, RES>> unregisterCallback) {
        this.topic = Objects.requireNonNull(topic, "topic cannot be null");
        this.requestClass = Objects.requireNonNull(requestClass, "requestClass cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.executor = Objects.requireNonNull(executor, "executor cannot be null");
        this.handler = Objects.requireNonNull(handler, "handler cannot be null");
        this.unregisterCallback = Objects.requireNonNull(unregisterCallback, "unregisterCallback cannot be null");
    }

    @Override
    @NotNull
    public String getTopic() {
        return topic;
    }

    @NotNull
    public Class<REQ> getRequestClass() {
        return requestClass;
    }

    @Override
    @NotNull
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public boolean isActive() {
        return active.get();
    }

    @Override
    public void unsubscribe() {
        if (active.compareAndSet(true, false)) {
            unregisterCallback.accept(this);
        }
    }

    public boolean matchesRequest(@NotNull Object request) {
        return requestClass.isInstance(request);
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<Object> dispatch(@NotNull MessageContext<?> context, @NotNull Object request) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                CompletableFuture<RES> handlerFuture = handler.handle((MessageContext<REQ>) context, (REQ) request);
                if (handlerFuture == null) {
                    future.completeExceptionally(new NullPointerException("RequestHandler returned null future"));
                } else {
                    handlerFuture.whenComplete((res, err) -> {
                        if (err != null) {
                            future.completeExceptionally(err);
                        } else {
                            future.complete(res);
                        }
                    });
                }
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
