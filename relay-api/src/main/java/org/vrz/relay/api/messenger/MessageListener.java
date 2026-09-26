package org.vrz.relay.api.messenger;

import org.jetbrains.annotations.NotNull;

/**
 * Functional callback interface for receiving messages on subscribed topics.
 *
 * @param <M> the expected payload type
 */
@FunctionalInterface
public interface MessageListener<M> {

    /**
     * Invoked when a message matching this subscriber's topic and payload type is received.
     *
     * @param context the envelope containing topic, payload, and metadata
     * @throws Exception any exception thrown during message handling (caught and logged by Relay)
     */
    void onMessage(@NotNull MessageContext<M> context) throws Exception;
}
