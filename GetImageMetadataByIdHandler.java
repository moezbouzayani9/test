package com.valuephone.image.management.images.handler;

import com.valuephone.image.dto.ImageDto;
import com.valuephone.image.dto.ImageEntity;
import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.management.images.ImageRegistry;
import com.valuephone.image.management.images.jdbc.ImageManagerJDBCUtills;
import com.valuephone.image.utilities.CheckUtilities;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

import java.util.Deque;
import java.util.Map;
import java.util.UUID;


@Slf4j
public class GetImageMetadataByIdHandler implements HttpHandler {

    private final ImageManagerJDBCUtills imageManagerDBUtills;
    private final ImageRegistry imageRegistry;
    private final ImageManagerHanlderUtilities imageManagerHanlderUtilities;


    public GetImageMetadataByIdHandler(ImageManagerJDBCUtills imageManagerDBUtills, ImageRegistry imageRegistry, ImageManagerHanlderUtilities imageManagerHanlderUtilities) {
        this.imageManagerDBUtills = imageManagerDBUtills;
        this.imageRegistry = imageRegistry;
        this.imageManagerHanlderUtilities = imageManagerHanlderUtilities;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        final Long id = HandlerUtilities.readLongParam(queryParameters, Constants.IMAGE_ID);

        CheckUtilities.checkArgumentNotNull(id, Constants.IMAGE_ID);

        ImageDto imageDtoById = getImageDtoById(id);

        imageManagerHanlderUtilities.sendObjectAsJsonResponse(exchange, imageDtoById);
    }

    public ImageDto getImageDtoById(long id) throws ValuePhoneException {
        if (id == -1)
            return null;

        ImageEntity imageEntity = imageManagerDBUtills.getImageById(id);
        UUID uuid = imageRegistry.registerImageIdAndReturnKey(id);

        return new ImageDto(uuid, imageEntity.getName());
    }

}
