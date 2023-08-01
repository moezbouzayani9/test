package com.valuephone.image.handler;

import com.valuephone.image.cache.ImageDeduplicationCache;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class ImageDeduplicationGetStatusReportForImageDeduplicationCacheHandler implements HttpHandler {

    private final ImageDeduplicationCache imageDeduplicationCache;

    public ImageDeduplicationGetStatusReportForImageDeduplicationCacheHandler(ImageDeduplicationCache imageDeduplicationCache) {
        this.imageDeduplicationCache = imageDeduplicationCache;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String stats = imageDeduplicationCache.getStatsForImageDeduplicationCache().toString();

        exchange.getResponseSender().send(stats);
        exchange.endExchange();
    }
}
