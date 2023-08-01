package com.valuephone.image.handler;

import com.valuephone.image.management.images.ImageRegistry;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class ImageRegistryClearCacheHandler implements HttpHandler {

    private final String CLEAR_CACHE_SUCCESSFUL = "BoImageRegistry successfully cleared.";

    private final ImageRegistry imageRegistry;

    public ImageRegistryClearCacheHandler(ImageRegistry imageRegistry) {
        this.imageRegistry = imageRegistry;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        imageRegistry.clearRegistry();

        exchange.getResponseSender().send(CLEAR_CACHE_SUCCESSFUL);
        exchange.endExchange();
    }
}
