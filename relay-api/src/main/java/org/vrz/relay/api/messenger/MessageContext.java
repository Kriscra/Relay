package org.vrz.relay.api.messenger;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Optional;

/**
 * Encapsulates the context of a delivered message over the Relay Messenger system.
 *
 * @param <M> the payload type
 */
public interface MessageContext<M> {

    /**
     * Gets the topic channel this message was published to.
     *
     * @return the topic identifier
     */
    @NotNull
    String getTopic();

    /**
     * Gets the typed payload of the message.
     *
     * @return payload object
     */
    @NotNull
    M getPayload();

    /**
     * Gets the timestamp when this message was created/dispatched.
     *
     * @return timestamp instant
     */
    @NotNull
    Instant getTimestamp();

    /**
     * Gets the plugin that published this message, if known.
     *
     * @return optional containing the publisher plugin
     */
    @NotNull
    Optional<Plugin> getPublisher();
}
