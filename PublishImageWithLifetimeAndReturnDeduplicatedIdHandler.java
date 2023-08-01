package com.valuephone.image.management.images.handler;

import com.valuephone.image.utilities.CheckUtilities;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Deque;
import java.util.Map;


@Slf4j
public class PublishImageWithLifetimeAndReturnDeduplicatedIdHandler implements HttpHandler {

    private final ImageManagerHanlderUtilities imageManagerHanlderUtilities;

    static final OffsetDateTime DEFAULT_VALID_TILL = OffsetDateTime.of(2050, 12, 31, 23, 59, 59, 0,
            ZoneOffset.of("+02:00"));

    public PublishImageWithLifetimeAndReturnDeduplicatedIdHandler(ImageManagerHanlderUtilities imageManagerHanlderUtilities) {
        this.imageManagerHanlderUtilities = imageManagerHanlderUtilities;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        final Long imageId = HandlerUtilities.readLongParam(queryParameters, Constants.IMAGE_ID);
        final OffsetDateTime validFrom = HandlerUtilities.readOffsetDateTimeParam(queryParameters, Constants.VALID_FROM);
        final OffsetDateTime validTill = HandlerUtilities.readOffsetDateTimeParam(queryParameters, Constants.VALID_TILL);

        CheckUtilities.checkArgumentNotNull(imageId, Constants.IMAGE_ID);

        long id = imageManagerHanlderUtilities.publishImageWithLifetimeAndReturnDeduplicatedId(imageId,
                validFrom != null ? validFrom : OffsetDateTime.now(),
                validTill != null ? validTill : DEFAULT_VALID_TILL);
        imageManagerHanlderUtilities.sendLongAsResponse(exchange, id);
    }


}
