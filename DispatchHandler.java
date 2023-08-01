package com.valuephone.image.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * @author tcigler
 * @since 1.0
 */
public class DispatchHandler implements HttpHandler {

    private final HttpHandler next;

    public DispatchHandler(final HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange httpServerExchange) throws Exception {

        // make processing non-blocking for incoming requests
        if (httpServerExchange.isInIoThread()) {
            httpServerExchange.dispatch(this);
            return;
        }

        next.handleRequest(httpServerExchange);

    }

}
