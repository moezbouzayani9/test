package com.valuephone.image.handler;

import com.valuephone.image.management.images.ImageRegistry;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class ImageRegistryGetStatusReportForImageLifetimeCacheHandler implements HttpHandler {

    private final ImageRegistry imageRegistry;

    public ImageRegistryGetStatusReportForImageLifetimeCacheHandler(ImageRegistry imageRegistry) {
        this.imageRegistry = imageRegistry;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String stats = imageRegistry.getStatsForImageRegistry().toString();

        exchange.getResponseSender().send(stats);
        exchange.endExchange();
    }
}
