package org.vrz.relay.messenger;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vrz.relay.api.messenger.MessageContext;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Concrete implementation of {@link MessageContext}.
 *
 * @param <M> payload type
 */
public final class RelayMessageContextImpl<M> implements MessageContext<M> {

    private final String topic;
    private final M payload;
    private final Instant timestamp;
    private final Plugin publisher;

    public RelayMessageContextImpl(@NotNull String topic,
                                   @NotNull M payload,
                                   @NotNull Instant timestamp,
                                   @Nullable Plugin publisher) {
        this.topic = Objects.requireNonNull(topic, "topic cannot be null");
        this.payload = Objects.requireNonNull(payload, "payload cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
        this.publisher = publisher;
    }

    @Override
    @NotNull
    public String getTopic() {
        return topic;
    }

    @Override
    @NotNull
    public M getPayload() {
        return payload;
    }

    @Override
    @NotNull
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    @NotNull
    public Optional<Plugin> getPublisher() {
        return Optional.ofNullable(publisher);
    }
}
