package org.vrz.relay.api.messenger;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Thread-safe inter-plugin publish-subscribe messaging and Request-Response (RPC) backbone.
 * <p>
 * Enables plugins to communicate dynamically without compile-time hard dependencies
 * on each other's classes or JAR files.
 *
 * <h3>1. Publish / Subscribe Pattern</h3>
 * <pre>{@code
 * // Publishing a broadcast message (non-blocking)
 * messenger.publish("clan:levelup", new ClanLevelUpPayload(clanId, 5), myPlugin);
 *
 * // Subscribing to a topic
 * Subscription sub = messenger.subscribe("clan:levelup", ClanLevelUpPayload.class, myPlugin, ctx -> {
 *     ClanLevelUpPayload data = ctx.getPayload();
 *     getLogger().info("Clan " + data.clanId() + " leveled up to " + data.level());
 * });
 * }</pre>
 *
 * <h3>2. Request / Response (RPC) Pattern</h3>
 * <pre>{@code
 * // Registering an RPC handler (Server/Provider side)
 * messenger.handleRequestSync("clan:get_info", UUID.class, myPlugin, (ctx, clanId) -> {
 *     Clan clan = clanManager.getClan(clanId);
 *     return new ClanInfoResponse(clan.getName(), clan.getLevel());
 * });
 *
 * // Sending an RPC request (Client/Consumer side)
 * messenger.request("clan:get_info", clanId, ClanInfoResponse.class, Duration.ofSeconds(2))
 *          .thenAccept(info -> player.sendMessage("Klan: " + info.name()))
 *          .exceptionally(err -> {
 *              player.sendMessage("Klan bilgisi alınamadı: " + err.getMessage());
 *              return null;
 *          });
 * }</pre>
 */
public interface RelayMessenger {

    /**
     * Publishes a message asynchronously to all active subscribers of the specified topic.
     *
     * @param topic     the target topic/channel identifier
     * @param payload   the message content/data to broadcast
     * @param publisher the plugin sending this message (or null if anonymous)
     * @return a CompletableFuture completed with the count of subscribers that received the message
     */
    @NotNull
    CompletableFuture<Integer> publish(@NotNull String topic,
                                      @NotNull Object payload,
                                      @Nullable Plugin publisher);

    /**
     * Publishes a message asynchronously without specifying a publisher plugin.
     *
     * @param topic   the target topic/channel identifier
     * @param payload the message content/data to broadcast
     * @return a CompletableFuture completed with the count of subscribers
     */
    @NotNull
    default CompletableFuture<Integer> publish(@NotNull String topic, @NotNull Object payload) {
        return publish(topic, payload, null);
    }

    /**
     * Subscribes to a topic. Messages will be dispatched to the listener on an asynchronous worker pool.
     *
     * @param topic        the topic channel to listen to
     * @param payloadClass the expected payload class token
     * @param subscriber   the plugin owning this subscription
     * @param listener     the callback consuming the message context
     * @param <M>          the payload type
     * @return a Subscription handle to manage or cancel this listener
     */
    @NotNull
    <M> Subscription subscribe(@NotNull String topic,
                              @NotNull Class<M> payloadClass,
                              @NotNull Plugin subscriber,
                              @NotNull MessageListener<M> listener);

    /**
     * Subscribes to a topic with a custom {@link Executor} (e.g. Bukkit main thread scheduler,
     * Folia region scheduler, or custom thread pool).
     *
     * @param topic        the topic channel to listen to
     * @param payloadClass the expected payload class token
     * @param subscriber   the plugin owning this subscription
     * @param executor     the executor on which the listener callback should run
     * @param listener     the callback consuming the message context
     * @param <M>          the payload type
     * @return a Subscription handle
     */
    @NotNull
    <M> Subscription subscribe(@NotNull String topic,
                              @NotNull Class<M> payloadClass,
                              @NotNull Plugin subscriber,
                              @NotNull Executor executor,
                              @NotNull MessageListener<M> listener);

    /**
     * Sends an RPC request to the registered request handler of the specified topic.
     *
     * @param topic          the target RPC topic channel
     * @param requestPayload the request parameters/object
     * @param responseClass  the expected response class type
     * @param timeout        maximum duration to wait for a response before timing out
     * @param requester      the plugin sending the request
     * @param <REQ>          request type
     * @param <RES>          response type
     * @return a future yielding the response, or failing with {@link RequestTimeoutException}
     */
    @NotNull
    <REQ, RES> CompletableFuture<RES> request(@NotNull String topic,
                                             @NotNull REQ requestPayload,
                                             @NotNull Class<RES> responseClass,
                                             @NotNull Duration timeout,
                                             @Nullable Plugin requester);

    /**
     * Sends an RPC request with a custom timeout.
     */
    @NotNull
    default <REQ, RES> CompletableFuture<RES> request(@NotNull String topic,
                                                     @NotNull REQ requestPayload,
                                                     @NotNull Class<RES> responseClass,
                                                     @NotNull Duration timeout) {
        return request(topic, requestPayload, responseClass, timeout, null);
    }

    /**
     * Sends an RPC request with a default 5-second timeout.
     */
    @NotNull
    default <REQ, RES> CompletableFuture<RES> request(@NotNull String topic,
                                                     @NotNull REQ requestPayload,
                                                     @NotNull Class<RES> responseClass) {
        return request(topic, requestPayload, responseClass, Duration.ofSeconds(5), null);
    }

    /**
     * Registers an asynchronous RPC request handler for the given topic.
     *
     * @param topic        the RPC topic channel to serve
     * @param requestClass the expected request class type
     * @param responder    the plugin providing this service
     * @param handler      the async handler function
     * @param <REQ>        request type
     * @param <RES>        response type
     * @return a Subscription handle to unregister this handler
     */
    @NotNull
    <REQ, RES> Subscription handleRequest(@NotNull String topic,
                                         @NotNull Class<REQ> requestClass,
                                         @NotNull Plugin responder,
                                         @NotNull RequestHandler<REQ, RES> handler);

    /**
     * Registers a synchronous RPC request handler for the given topic.
     *
     * @param topic        the RPC topic channel to serve
     * @param requestClass the expected request class type
     * @param responder    the plugin providing this service
     * @param syncHandler  the sync handler function
     * @param <REQ>        request type
     * @param <RES>        response type
     * @return a Subscription handle to unregister this handler
     */
    @NotNull
    default <REQ, RES> Subscription handleRequestSync(@NotNull String topic,
                                                     @NotNull Class<REQ> requestClass,
                                                     @NotNull Plugin responder,
                                                     @NotNull SyncRequestHandler<REQ, RES> syncHandler) {
        return handleRequest(topic, requestClass, responder, (ctx, req) -> {
            try {
                return CompletableFuture.completedFuture(syncHandler.handle(ctx, req));
            } catch (Throwable t) {
                return CompletableFuture.failedFuture(t);
            }
        });
    }

    /**
     * Registers an RPC request handler executed on a custom {@link Executor}.
     */
    @NotNull
    <REQ, RES> Subscription handleRequest(@NotNull String topic,
                                         @NotNull Class<REQ> requestClass,
                                         @NotNull Plugin responder,
                                         @NotNull Executor executor,
                                         @NotNull RequestHandler<REQ, RES> handler);

    /**
     * Checks if there is an active request handler registered for the specified topic.
     *
     * @param topic the topic to check
     * @return true if an RPC handler is registered
     */
    boolean hasRequestHandler(@NotNull String topic);

    /**
     * Unregisters and cancels all active subscriptions and RPC handlers owned by the specified plugin.
     * Recommended to call during plugin shutdown.
     *
     * @param plugin the plugin to clean up
     */
    void unsubscribeAll(@NotNull Plugin plugin);

    /**
     * Gets a snapshot set of all topics currently having at least one active subscriber or request handler.
     *
     * @return unmodifiable set of topic names
     */
    @NotNull
    Set<String> getActiveTopics();

    /**
     * Returns the number of active broadcast subscribers currently listening to the given topic.
     *
     * @param topic the topic to query
     * @return subscriber count
     */
    int getSubscriberCount(@NotNull String topic);
}
