package com.valuephone.image.management.images.handler;

import com.valuephone.image.dto.ImageDto;
import com.valuephone.image.dto.ImageEntity;
import com.valuephone.image.exception.ImageException;
import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.helper.CustomImageReader;
import com.valuephone.image.management.images.ImageConfigurationManager;
import com.valuephone.image.management.images.ImageRegistry;
import com.valuephone.image.management.images.jdbc.ImageManagerJDBCUtills;
import com.valuephone.image.management.share.images.ImageConfiguration;
import com.valuephone.image.management.share.images.ImageType;
import com.valuephone.image.utilities.CheckUtilities;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.BlockingHttpExchange;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;


@Slf4j
public class UploadImageForImageTypeHandler implements HttpHandler {

    private final ImageManagerHanlderUtilities imageManagerHanlderUtilities;
    private final ImageManagerJDBCUtills imageManagerDBUtills;
    private final ImageConfigurationManager imageConfigurationManager;
    private final ImageRegistry imageRegistry;


    public UploadImageForImageTypeHandler(ImageManagerHanlderUtilities imageManagerHanlderUtilities, ImageManagerJDBCUtills imageManagerDBUtills, ImageConfigurationManager imageConfigurationManager, ImageRegistry imageRegistry) {
        this.imageManagerHanlderUtilities = imageManagerHanlderUtilities;
        this.imageManagerDBUtills = imageManagerDBUtills;
        this.imageConfigurationManager = imageConfigurationManager;
        this.imageRegistry = imageRegistry;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        final String imageName = HandlerUtilities.readStringParam(queryParameters, Constants.IMAGE_NAME);
        final String mimeType = HandlerUtilities.readStringParam(queryParameters, Constants.MIME_TYPE);
        final ImageType imageType = HandlerUtilities.readEnumParam(queryParameters, Constants.IMAGE_TYPE, ImageType.class);

        CheckUtilities.checkArgumentNotNull(imageName, Constants.IMAGE_NAME);
        CheckUtilities.checkArgumentNotNull(mimeType, Constants.MIME_TYPE);
        CheckUtilities.checkArgumentNotNull(imageType, Constants.IMAGE_TYPE);

        try (BlockingHttpExchange ignored = exchange.startBlocking();
             InputStream inputStream = exchange.getInputStream()) {

            if (inputStream == null) {
                log.warn("no input image");
                throw new ImageException("no input image");
            }

            final CustomImageReader reader = new CustomImageReader(inputStream);
            byte[] imageData = reader.getImageBytes();

            ImageDto imageDto = uploadImageForBoImageType(imageName, mimeType, imageData, imageType);

            imageManagerHanlderUtilities.sendObjectAsJsonResponse(exchange, imageDto, false);

        } catch (IOException e) {
            log.error("can't read input image", e);
            throw new ImageException("can't read input image", e);
        }
        exchange.endExchange();
    }

    public ImageDto uploadImageForBoImageType(String imageName, String mimeType, byte[] imageData,
                                              ImageType imageType) throws ValuePhoneException {
        ImageEntity imageEntity = imageManagerHanlderUtilities.registerImageForImageType(imageName, mimeType, imageType);
        Long imageId = imageEntity.getId();

        ImageConfiguration imageConfiguration = imageConfigurationManager.getImageConfiguration(imageType);
        byte[] scaledImage = imageManagerHanlderUtilities.scaleImage(imageData, mimeType,
                imageConfiguration.getMaxWidth(), imageConfiguration.getMaxHeight(), 0.95f);

        imageEntity = imageManagerHanlderUtilities.deduplicateImageForDataAndReturnDeduplicatedEntity(imageEntity, scaledImage);
        imageEntity.setModificationDate(OffsetDateTime.now());
        if (Objects.equals(imageId, imageEntity.getDeduplicatedImageId()))
            imageManagerDBUtills.persistImageData(imageEntity, scaledImage);

        UUID uuid = imageRegistry.registerImageIdAndReturnKey(imageId);
        return new ImageDto(uuid, imageName);
    }

}
