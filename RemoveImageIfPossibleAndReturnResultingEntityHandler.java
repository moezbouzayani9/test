package com.valuephone.image.management.images.handler;

import com.google.common.hash.HashCode;
import com.valuephone.image.dto.ImageEntity;
import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.management.images.ImageDeduplicationKey;
import com.valuephone.image.management.images.ImageDeduplicationManager;
import com.valuephone.image.management.images.jdbc.ImageManagerJDBCUtills;
import com.valuephone.image.utilities.CheckUtilities;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;


@Slf4j
public class RemoveImageIfPossibleAndReturnResultingEntityHandler implements HttpHandler {

    private final ImageManagerHanlderUtilities imageManagerHanlderUtilities;
    private final ImageManagerJDBCUtills imageManagerDBUtills;
    private final ImageDeduplicationManager imageDeduplicationManager;

    public RemoveImageIfPossibleAndReturnResultingEntityHandler(ImageManagerHanlderUtilities imageManagerHanlderUtilities, ImageManagerJDBCUtills imageManagerDBUtills, ImageDeduplicationManager imageDeduplicationManager) {
        this.imageManagerHanlderUtilities = imageManagerHanlderUtilities;
        this.imageManagerDBUtills = imageManagerDBUtills;
        this.imageDeduplicationManager = imageDeduplicationManager;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        final Long imageId = HandlerUtilities.readLongParam(queryParameters, Constants.IMAGE_ID);

        CheckUtilities.checkArgumentNotNull(imageId, Constants.IMAGE_ID);

        ImageEntity retVal = removeImageIfPossibleAndReturnResultingEntity(imageId);

        imageManagerHanlderUtilities.sendObjectAsJsonResponse(exchange, retVal);
    }

    private boolean isOriginalImage(ImageEntity imageEntity) {
        return imageEntity.getId() == imageEntity.getDeduplicatedImageId();
    }

    public ImageEntity removeImageIfPossibleAndReturnResultingEntity(Long imageId)
            throws ValuePhoneException {
        ImageEntity imageEntity = imageManagerDBUtills.getImageById(imageId);
        if (imageEntity.getValidTill() != null && imageEntity.getValidTill().isAfter(OffsetDateTime.now()))
            log.info("The image[" + imageEntity.getId() + "] should be removed, but it's still valid.");

        imageEntity.setDeleted(true);
        imageManagerDBUtills.updateImageEntity(imageEntity);

        if (isOriginalImage(imageEntity)) {
            ImageDeduplicationKey key = ImageDeduplicationManager
                    .createAndReturnDeduplicationKeyForImageSizeAndHashCodeAndType(
                            imageEntity.getImageSize(),
                            HashCode.fromString(imageEntity.getImageHash()), imageEntity.getImageType());

            imageDeduplicationManager.removeCachedEntryForDeduplicationKey(key);
        }

        Long deduplicatedImageId = imageEntity.getDeduplicatedImageId();

        if (Objects.nonNull(deduplicatedImageId) && imageManagerDBUtills.isOrphanedImageData(deduplicatedImageId))
            imageManagerDBUtills.removeImageDataForDeduplicatedImageId(deduplicatedImageId);

        log.debug("removed Image[" + imageEntity.getId() + "]");
        return imageEntity;
    }

}
