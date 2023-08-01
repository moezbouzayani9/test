package com.valuephone.image.management.images.handler;

import com.valuephone.image.dto.ImageEntity;
import com.valuephone.image.utilities.CheckUtilities;
import com.valuephone.image.utilities.Constants;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class DirectCopyHandler implements HttpHandler {

    private final ImageManagerHanlderUtilities imageManagerHanlderUtilities;

    public DirectCopyHandler(ImageManagerHanlderUtilities imageManagerHanlderUtilities) {
        this.imageManagerHanlderUtilities = imageManagerHanlderUtilities;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        ImageEntity imageEntity = imageManagerHanlderUtilities.readInputJson(exchange, ImageEntity.class);

        CheckUtilities.checkArgumentNotNull(imageEntity, Constants.IMAGE_ENTITY);
        imageEntity = imageManagerHanlderUtilities.directCopy(imageEntity);
        imageManagerHanlderUtilities.sendObjectAsJsonResponse(exchange, imageEntity);

    }


}
