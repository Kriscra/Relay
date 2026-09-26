package org.vrz.relay.api.event;

import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Base abstract event for all Relay-fired events.
 * Supports synchronous or asynchronous dispatch.
 */
public abstract class RelayEvent extends Event {

    public RelayEvent() {
        super();
    }

    public RelayEvent(boolean isAsync) {
        super(isAsync);
    }
}
