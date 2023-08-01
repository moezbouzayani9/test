package com.valuephone.image.management.images.handler;

import com.valuephone.image.dto.ImageEntity;
import com.valuephone.image.exception.ImageException;
import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.helper.ImageServiceManager;
import com.valuephone.image.management.images.ImageDeduplicationManager;
import com.valuephone.image.management.images.ImageLifetime;
import com.valuephone.image.management.images.ImageLifetimeKey;
import com.valuephone.image.management.images.jdbc.ImageManagerJDBCUtills;
import com.valuephone.image.management.share.images.ImagePoolType;
import com.valuephone.image.utilities.CheckUtilities;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.DatabaseManager;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;


@Slf4j
public class RevokeImageAndReturnResultingEntityHandler implements HttpHandler {

    private final ImageManagerHanlderUtilities imageManagerHanlderUtilities;
    private final ImageManagerJDBCUtills imageManagerDBUtills;
    private final Integer endOfLifeTimeout;
    private final ImageDeduplicationManager imageDeduplicationManager;
    private final DatabaseManager imageDatabaseManager;

    public RevokeImageAndReturnResultingEntityHandler(ImageManagerHanlderUtilities imageManagerHanlderUtilities, ImageManagerJDBCUtills imageManagerDBUtills, Integer endOfLifeTimeout, ImageDeduplicationManager imageDeduplicationManager, DatabaseManager imageDatabaseManager) {
        this.imageManagerHanlderUtilities = imageManagerHanlderUtilities;
        this.imageManagerDBUtills = imageManagerDBUtills;
        this.endOfLifeTimeout = endOfLifeTimeout;
        this.imageDeduplicationManager = imageDeduplicationManager;
        this.imageDatabaseManager = imageDatabaseManager;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        final Long imageId = HandlerUtilities.readLongParam(queryParameters, Constants.IMAGE_ID);

        CheckUtilities.checkArgumentNotNull(imageId, Constants.IMAGE_ID);

        ImageEntity image = revokeImageAndReturnResultingEntity(imageId);

        imageManagerHanlderUtilities.sendObjectAsJsonResponse(exchange, image);
    }

    private boolean isPublishedImage(ImageEntity imageEntity) {
        return imageEntity.getValidFrom() != null || imageEntity.getValidTill() != null;
    }

    public ImageEntity revokeImageAndReturnResultingEntity(Long imageId) throws ValuePhoneException {
        try {
            ImageEntity imageEntity = imageManagerDBUtills.getImageById(imageId);
            if (!isPublishedImage(imageEntity)) {
                return imageEntity;
            }
            if (Objects.isNull(imageEntity.getDeduplicatedImageId()) && !imageEntity.isDeleted()) {
                imageEntity.setDeleted(true);
                imageManagerDBUtills.updateImageEntity(imageEntity);
                return imageEntity;
            }

            imageEntity.setValidFrom(null);
            imageEntity.setValidTill(null);
            imageEntity.setEndOfLife(LocalDate.now().plusDays(this.endOfLifeTimeout));
            imageManagerDBUtills.updateImageEntity(imageEntity);

            ImageLifetimeKey lifetimeKey = new ImageLifetimeKey(imageEntity.getDeduplicatedImageId());

            ImageLifetime currentImageLifetime = imageDeduplicationManager.getLifetimeForImageLifetimeKey(lifetimeKey);

            ImageLifetime referencingLifetime = imageManagerDBUtills.getRemainingImageLifetimeForImageLifetimeKey(lifetimeKey, imageEntity.getId());

            if (!imageManagerHanlderUtilities.isInvalidLifetime(referencingLifetime)) {
                if (!currentImageLifetime.equals(referencingLifetime))
                    imageManagerHanlderUtilities.modifyLifetimeForImageOnAppServerAndCacheAndReturnTrueIfSuccessful(currentImageLifetime,
                            referencingLifetime, lifetimeKey);
            } else {
                long deduplicatedImageId = imageEntity.getDeduplicatedImageId();
                ImagePoolType imagePoolType = imageEntity.getImageType().getImagePoolType();

                log.debug("revokeImageById(" + imageEntity.getId() + ") -> " +
                        "deleteImage(" + deduplicatedImageId + ", " + imagePoolType.name() + ")");

                try (Connection connection = imageDatabaseManager.getConnection()) {
                    ImageServiceManager.removeImage(deduplicatedImageId, connection);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                    throw new IllegalStateException("SQL exception");
                }

                imageDeduplicationManager.removeCachedEntryForImageLifetimeKey(lifetimeKey);
            }
            return imageEntity;
        } catch (ImageException e) {
            log.error(e.getMessage(), e);
            throw new ValuePhoneException(e, "FAILED to revokeImageAndReturnResultingEntity()");
        }
    }

}
