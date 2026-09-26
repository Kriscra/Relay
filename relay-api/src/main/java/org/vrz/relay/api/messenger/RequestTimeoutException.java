package org.vrz.relay.api.messenger;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Thrown when a request sent via {@link RelayMessenger#request} does not receive
 * a response within the configured timeout duration.
 */
public class RequestTimeoutException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String topic;
    private final Duration timeout;

    public RequestTimeoutException(@NotNull String topic, @NotNull Duration timeout) {
        super("Request to topic '" + topic + "' timed out after " + timeout.toMillis() + "ms without response.");
        this.topic = topic;
        this.timeout = timeout;
    }

    @NotNull
    public String getTopic() {
        return topic;
    }

    @NotNull
    public Duration getTimeout() {
        return timeout;
    }
}
