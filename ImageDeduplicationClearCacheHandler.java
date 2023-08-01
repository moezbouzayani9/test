package com.valuephone.image.handler;

import com.valuephone.image.cache.ImageDeduplicationCache;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class ImageDeduplicationClearCacheHandler implements HttpHandler {

    private final String CLEAR_CACHE_SUCCESSFUL = "BoImageDeduplicationCache successfully cleared.";

    private final ImageDeduplicationCache imageDeduplicationCache;

    public ImageDeduplicationClearCacheHandler(ImageDeduplicationCache imageDeduplicationCache) {
        this.imageDeduplicationCache = imageDeduplicationCache;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        imageDeduplicationCache.clearCache();

        exchange.getResponseSender().send(CLEAR_CACHE_SUCCESSFUL);
        exchange.endExchange();
    }
}
