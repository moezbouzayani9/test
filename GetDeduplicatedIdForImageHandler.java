package com.valuephone.image.management.images.handler;

import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.management.images.jdbc.ImageManagerJDBCUtills;
import com.valuephone.image.utilities.CheckUtilities;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

import java.util.Deque;
import java.util.Map;


@Slf4j
public class GetDeduplicatedIdForImageHandler implements HttpHandler {

    private final ImageManagerHanlderUtilities imageManagerHanlderUtilities;
    private final ImageManagerJDBCUtills imageManagerDBUtills;

    public GetDeduplicatedIdForImageHandler(ImageManagerHanlderUtilities imageManagerHanlderUtilities, ImageManagerJDBCUtills imageManagerDBUtills) {
        this.imageManagerHanlderUtilities = imageManagerHanlderUtilities;
        this.imageManagerDBUtills = imageManagerDBUtills;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        final Long imageId = HandlerUtilities.readLongParam(queryParameters, Constants.IMAGE_ID);

        CheckUtilities.checkArgumentNotNull(imageId, Constants.IMAGE_ID);

        long retVal = getDeduplicatedIdForImage(imageId);
        imageManagerHanlderUtilities.sendLongAsResponse(exchange, retVal);
    }

    public long getDeduplicatedIdForImage(long imageId)
            throws ValuePhoneException {
        return imageManagerDBUtills.getImageById(imageId).getDeduplicatedImageId();
    }

}
