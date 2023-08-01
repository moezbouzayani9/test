package com.valuephone.image.management.images.handler;

import com.valuephone.image.dto.ImageConfigurationDto;
import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.management.images.ImageConfigurationManager;
import com.valuephone.image.management.share.images.ImageConfiguration;
import com.valuephone.image.management.share.images.ImageType;
import com.valuephone.image.utilities.CheckUtilities;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

import java.util.Deque;
import java.util.Map;


@Slf4j
public class GetImageConfigurationDtoHandler implements HttpHandler {

    private final ImageManagerHanlderUtilities imageManagerHanlderUtilities;
    private final ImageConfigurationManager imageConfigurationManager;

    public GetImageConfigurationDtoHandler(ImageManagerHanlderUtilities imageManagerHanlderUtilities, ImageConfigurationManager imageConfigurationManager) {
        this.imageManagerHanlderUtilities = imageManagerHanlderUtilities;
        this.imageConfigurationManager = imageConfigurationManager;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        final ImageType imageType;
        try {
            imageType = ImageType.valueOf(HandlerUtilities.readStringParam(queryParameters, Constants.IMAGE_TYPE));
        } catch (IllegalArgumentException e) {
            log.warn("can't get image type", e);
            throw new ValuePhoneException("can't get image type");
        }
        CheckUtilities.checkArgumentNotNull(imageType, Constants.IMAGE_TYPE);

        ImageConfigurationDto retVal = getImageConfigurationDto(imageType);
        imageManagerHanlderUtilities.sendObjectAsJsonResponse(exchange, retVal);
    }

    public ImageConfigurationDto getImageConfigurationDto(ImageType imageType) throws ValuePhoneException {
        ImageConfiguration imageConfiguration = imageConfigurationManager.getImageConfiguration(imageType);
        return new ImageConfigurationDto(imageConfiguration.getMinWidth(), imageConfiguration.getMaxWidth(), imageConfiguration.getMinHeight(), imageConfiguration.getMaxHeight(), imageConfiguration.getMaxFileSize());
    }

}
