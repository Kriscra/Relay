package org.vrz.relay.api.messenger;

import org.jetbrains.annotations.NotNull;

/**
 * Functional interface for synchronously processing incoming RPC requests.
 *
 * @param <REQ> request payload type
 * @param <RES> response payload type
 */
@FunctionalInterface
public interface SyncRequestHandler<REQ, RES> {

    /**
     * Handles an incoming request and immediately returns the response.
     *
     * @param context the message context
     * @param request the request payload
     * @return response payload
     * @throws Exception if an error occurs while handling the request
     */
    @NotNull
    RES handle(@NotNull MessageContext<REQ> context, @NotNull REQ request) throws Exception;
}
