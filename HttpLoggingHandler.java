package com.valuephone.image.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author dlu
 */
@Slf4j
public class HttpLoggingHandler implements HttpHandler {

    private final HttpHandler next;

    public HttpLoggingHandler(HttpHandler pathHandler) {
        // AccessLogHandler is used to log info at the end when the whole process is finished.
        this.next = new AccessLogHandler(pathHandler, log::info, "common", HttpLoggingHandler.class.getClassLoader());
    }

    @Override
    public final void handleRequest(final HttpServerExchange exchange) throws Exception {

        log.info("Incoming request: [{} - {}?{}]", exchange.getRequestMethod(), exchange.getRequestURI(), exchange.getQueryString());

        next.handleRequest(exchange);
    }
}
