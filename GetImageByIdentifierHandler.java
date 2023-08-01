package com.valuephone.image.management.images.handler;

import com.valuephone.image.dto.ImageEntity;
import com.valuephone.image.management.images.IdType;
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
public class GetImageByIdentifierHandler implements HttpHandler {


    private final ImageManagerHanlderUtilities imageManagerHanlderUtilities;
    private final ImageManagerJDBCUtills imageManagerDBUtills;

    public GetImageByIdentifierHandler(ImageManagerHanlderUtilities imageManagerHanlderUtilities, ImageManagerJDBCUtills imageManagerDBUtills) {
        this.imageManagerHanlderUtilities = imageManagerHanlderUtilities;
        this.imageManagerDBUtills = imageManagerDBUtills;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        final String identifier = HandlerUtilities.readStringParam(queryParameters, Constants.ID);
        final IdType idType = HandlerUtilities.readEnumParam(queryParameters, Constants.ID_TYPE, IdType.class);

        CheckUtilities.checkArgumentNotNull(identifier, Constants.ID);
        CheckUtilities.checkArgumentNotNull(idType, Constants.ID_TYPE);

        long id;
        if (IdType.UUID.equals(idType)) {
            id = imageManagerHanlderUtilities.getIdByUUID(identifier);
        } else {
            id = Long.parseLong(identifier);
        }

        ImageEntity image = imageManagerDBUtills.getImageById(id);
        imageManagerHanlderUtilities.sendObjectAsJsonResponse(exchange, image);
    }
}
