package org.vrz.relay.api.messenger;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Handle representing an active topic subscription in the Relay Messenger.
 * Can be used to inspect or cancel the subscription.
 */
public interface Subscription extends AutoCloseable {

    /**
     * Gets the topic channel this subscription is listening to.
     *
     * @return topic string
     */
    @NotNull
    String getTopic();

    /**
     * Gets the plugin that registered this subscription.
     *
     * @return owning plugin
     */
    @NotNull
    Plugin getPlugin();

    /**
     * Checks if this subscription is still actively receiving messages.
     *
     * @return true if active, false if cancelled
     */
    boolean isActive();

    /**
     * Cancels this subscription, stopping any further message deliveries.
     */
    void unsubscribe();

    /**
     * Closes this subscription (alias for {@link #unsubscribe()}).
     */
    @Override
    default void close() {
        unsubscribe();
    }
}
