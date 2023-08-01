package com.valuephone.image.management.images.handler;

import com.valuephone.image.utilities.CheckUtilities;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

import java.util.Deque;
import java.util.Map;


@Slf4j
public class GetIdByUUIDHandler implements HttpHandler {


    private final ImageManagerHanlderUtilities imageManagerHanlderUtilities;

    public GetIdByUUIDHandler(ImageManagerHanlderUtilities imageManagerHanlderUtilities) {
        this.imageManagerHanlderUtilities = imageManagerHanlderUtilities;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        final String uuid = HandlerUtilities.readStringParam(queryParameters, Constants.UUID);

        CheckUtilities.checkArgumentNotNull(uuid, Constants.UUID);

        long id = imageManagerHanlderUtilities.getIdByUUID(uuid);
        imageManagerHanlderUtilities.sendLongAsResponse(exchange, id);
    }


}
