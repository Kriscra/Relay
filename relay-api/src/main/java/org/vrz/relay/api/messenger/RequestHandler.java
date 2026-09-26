package org.vrz.relay.api.messenger;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Functional interface for asynchronously processing incoming RPC requests.
 *
 * @param <REQ> the request payload type
 * @param <RES> the response payload type
 */
@FunctionalInterface
public interface RequestHandler<REQ, RES> {

    /**
     * Handles an incoming request and returns a future completing with the response.
     *
     * @param context the message context
     * @param request the request payload
     * @return future yielding the response payload
     */
    @NotNull
    CompletableFuture<RES> handle(@NotNull MessageContext<REQ> context, @NotNull REQ request);
}
