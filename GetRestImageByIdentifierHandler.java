package com.valuephone.image.management.images.handler;

import com.valuephone.image.dto.ImageEntity;
import com.valuephone.image.dto.ImageMimeType;
import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.management.images.IdType;
import com.valuephone.image.management.images.RestImage;
import com.valuephone.image.management.images.jdbc.ImageManagerJDBCUtills;
import com.valuephone.image.utilities.CheckUtilities;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Deque;
import java.util.Map;


@Slf4j
public class GetRestImageByIdentifierHandler implements HttpHandler {

    private final ImageManagerHanlderUtilities imageManagerHanlderUtilities;
    private final ImageManagerJDBCUtills imageManagerDBUtills;

    public GetRestImageByIdentifierHandler(ImageManagerHanlderUtilities imageManagerHanlderUtilities, ImageManagerJDBCUtills imageManagerDBUtills) {
        this.imageManagerHanlderUtilities = imageManagerHanlderUtilities;
        this.imageManagerDBUtills = imageManagerDBUtills;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        final String id = HandlerUtilities.readStringParam(queryParameters, Constants.ID);
        final Integer maxWidth = HandlerUtilities.readIntegerParam(queryParameters, Constants.MAX_WIDTH);
        final Integer maxHeight = HandlerUtilities.readIntegerParam(queryParameters, Constants.MAX_HEIGHT);
        final Float quality = HandlerUtilities.readFloatParam(queryParameters, Constants.QUALITY);
        final IdType idType = HandlerUtilities.readEnumParam(queryParameters, Constants.ID_TYPE, IdType.class);

        CheckUtilities.checkArgumentNotNull(id, Constants.ID);
        CheckUtilities.checkArgumentNotNull(idType, Constants.ID_TYPE);

        RestImage restImage = getRestImageByIdentifier(id, idType, maxWidth, maxHeight, quality);
        HandlerUtilities.sendImageBytesAsResponse(exchange, ImageMimeType.fromString(restImage.mimeType), restImage.imageData);
    }

    public RestImage getRestImageByIdentifier(String identifier, IdType idType, Integer maxWidth, Integer maxHeight,
                                              Float quality) throws ValuePhoneException {

        long id;
        if (IdType.IMAGE_ID.equals(idType)) {
            id = Long.parseLong(identifier);
        } else {
            id = imageManagerHanlderUtilities.getIdByUUID(identifier);
        }

        ImageEntity ie = imageManagerDBUtills.getImageById(id);

        try {
            String mime = ie.getMimeType();
            return new RestImage(mime, imageManagerHanlderUtilities.getImageBytes(ie.getId(), mime, maxWidth, maxHeight, quality));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new ValuePhoneException(e, "FAILED to get binary data for image[" + identifier + "] -> [" + ie.getId()
                    + "]");
        }
    }

}
