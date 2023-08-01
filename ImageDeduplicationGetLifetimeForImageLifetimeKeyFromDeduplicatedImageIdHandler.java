package com.valuephone.image.handler;

import com.valuephone.image.cache.ImageDeduplicationCache;
import com.valuephone.image.management.images.ImageDeduplicationManager;
import com.valuephone.image.management.images.ImageLifetimeKey;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.util.Deque;
import java.util.Map;

public class ImageDeduplicationGetLifetimeForImageLifetimeKeyFromDeduplicatedImageIdHandler implements HttpHandler {

    private final ImageDeduplicationCache imageDeduplicationCache;

    public ImageDeduplicationGetLifetimeForImageLifetimeKeyFromDeduplicatedImageIdHandler(ImageDeduplicationCache imageDeduplicationCacheBean) {
        this.imageDeduplicationCache = imageDeduplicationCacheBean;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        final Long deduplicatedImageId = HandlerUtilities.readLongParam(queryParameters, Constants.DEDUPLICATED_IMAGE_ID);

        ImageLifetimeKey imageLifetimeKey = ImageDeduplicationManager
                .createAndReturnLifetimeKeyForDeduplicatedImageIdAndType(deduplicatedImageId);

        String lifetime = imageDeduplicationCache.getLifetimeForLifetimeKey(imageLifetimeKey).toString();

        exchange.getResponseSender().send(lifetime);
        exchange.endExchange();
    }
}
