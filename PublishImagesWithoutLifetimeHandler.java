package com.valuephone.image.handler;

import com.google.common.collect.ImmutableMap;
import com.valuephone.image.management.images.handler.ImageManagerHanlderUtilities;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Deque;
import java.util.Map;

public class PublishImagesWithoutLifetimeHandler implements HttpHandler {

    private static final String FINISHED_TO_PUBLISH_IMAGE_WITH_LIFETIME_AND_RETURN_DEDUPLICATED_ID =
            "FINISHED to publishImagesWithoutLifetimeAndReturnDeduplicatedIds(%s) RETURNED[%s]";

    static final OffsetDateTime DEFAULT_VALID_TILL = OffsetDateTime.of(2050, 12, 31, 23, 59, 59, 0,
            ZoneOffset.of("+02:00"));

    private final ImageManagerHanlderUtilities imageManagerHanlderUtilities;

    public PublishImagesWithoutLifetimeHandler(ImageManagerHanlderUtilities imageManagerHanlderUtilities) {
        this.imageManagerHanlderUtilities = imageManagerHanlderUtilities;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        final String imageIds = HandlerUtilities.readStringParam(queryParameters, Constants.IMAGE_IDS);
        ImmutableMap.Builder<Long, Long> b = ImmutableMap.builder();
        OffsetDateTime validFrom = OffsetDateTime.now();

        for (String imageId : imageIds.split("[,;]")) {
            long imgId = Long.parseLong(imageId.trim());
            b.put(imgId, imageManagerHanlderUtilities.publishImageWithLifetimeAndReturnDeduplicatedId(imgId, validFrom, DEFAULT_VALID_TILL));
        }

        imageManagerHanlderUtilities.sendObjectAsJsonResponse(
                exchange,
                String.format(FINISHED_TO_PUBLISH_IMAGE_WITH_LIFETIME_AND_RETURN_DEDUPLICATED_ID,
                        imageIds, b.build())
        );
    }
}
