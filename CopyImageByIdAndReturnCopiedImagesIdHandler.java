package com.valuephone.image.management.images.handler;

import com.valuephone.image.dto.ImageEntity;
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
public class CopyImageByIdAndReturnCopiedImagesIdHandler implements HttpHandler {

    private final ImageManagerHanlderUtilities imageManagerHanlderUtilities;
    private final ImageManagerJDBCUtills imageManagerDBUtills;


    public CopyImageByIdAndReturnCopiedImagesIdHandler(ImageManagerHanlderUtilities imageManagerHanlderUtilities, ImageManagerJDBCUtills imageManagerDBUtills) {
        this.imageManagerHanlderUtilities = imageManagerHanlderUtilities;
        this.imageManagerDBUtills = imageManagerDBUtills;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        final Long originalImagesId = HandlerUtilities.readLongParam(queryParameters, Constants.IMAGE_ID);

        CheckUtilities.checkArgumentNotNull(originalImagesId, Constants.IMAGE_ID);

        long retVal = copyImageByIdAndReturnCopiedImagesId(originalImagesId);

        imageManagerHanlderUtilities.sendLongAsResponse(exchange, retVal);
    }

    public long copyImageByIdAndReturnCopiedImagesId(long originalImagesId)
            throws ValuePhoneException {
        ImageEntity originalImage = imageManagerDBUtills.getImageById(originalImagesId);

        return !isDeletedImage(originalImagesId, originalImage) ?
                imageManagerHanlderUtilities.directCopy(originalImage).getId() : -1;
    }

    private boolean isDeletedImage(long originalImagesId, ImageEntity originalImage) {
        return originalImagesId == -1 || originalImage.isDeleted();
    }


}
