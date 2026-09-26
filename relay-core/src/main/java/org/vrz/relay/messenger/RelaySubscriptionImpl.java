package org.vrz.relay.messenger;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.messenger.MessageContext;
import org.vrz.relay.api.messenger.MessageListener;
import org.vrz.relay.api.messenger.Subscription;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Concrete implementation of {@link Subscription}.
 *
 * @param <M> payload type
 */
public final class RelaySubscriptionImpl<M> implements Subscription {

    private final String topic;
    private final Class<M> payloadClass;
    private final Plugin plugin;
    private final Executor executor;
    private final MessageListener<M> listener;
    private final Consumer<RelaySubscriptionImpl<M>> unregisterCallback;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public RelaySubscriptionImpl(@NotNull String topic,
                                 @NotNull Class<M> payloadClass,
                                 @NotNull Plugin plugin,
                                 @NotNull Executor executor,
                                 @NotNull MessageListener<M> listener,
                                 @NotNull Consumer<RelaySubscriptionImpl<M>> unregisterCallback) {
        this.topic = Objects.requireNonNull(topic, "topic cannot be null");
        this.payloadClass = Objects.requireNonNull(payloadClass, "payloadClass cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.executor = Objects.requireNonNull(executor, "executor cannot be null");
        this.listener = Objects.requireNonNull(listener, "listener cannot be null");
        this.unregisterCallback = Objects.requireNonNull(unregisterCallback, "unregisterCallback cannot be null");
    }

    @Override
    @NotNull
    public String getTopic() {
        return topic;
    }

    @NotNull
    public Class<M> getPayloadClass() {
        return payloadClass;
    }

    @Override
    @NotNull
    public Plugin getPlugin() {
        return plugin;
    }

    @NotNull
    public Executor getExecutor() {
        return executor;
    }

    @NotNull
    public MessageListener<M> getListener() {
        return listener;
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

    @SuppressWarnings("unchecked")
    public boolean matchesPayload(@NotNull Object payload) {
        return payloadClass.isInstance(payload);
    }

    @SuppressWarnings("unchecked")
    public void dispatch(@NotNull MessageContext<?> context) throws Exception {
        if (isActive() && matchesPayload(context.getPayload())) {
            listener.onMessage((MessageContext<M>) context);
        }
    }
}
