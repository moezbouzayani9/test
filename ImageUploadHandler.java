package com.valuephone.image.handler;

import com.valuephone.image.dto.ImageMetadata;
import com.valuephone.image.exception.ImageException;
import com.valuephone.image.exception.ImageNotFoundException;
import com.valuephone.image.helper.CustomImageReader;
import com.valuephone.image.helper.ImageServiceManager;
import com.valuephone.image.utilities.CheckUtilities;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.DatabaseManager;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Deque;
import java.util.Map;

/**
 * @author tcigler
 * @since 1.0
 */
public class ImageUploadHandler implements HttpHandler {

    private final DatabaseManager imageDatabaseManager;

    public ImageUploadHandler(final DatabaseManager imageDatabaseManager) {
        this.imageDatabaseManager = imageDatabaseManager;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        // part of the path
        final String poolId = HandlerUtilities.readStringParam(queryParameters, Constants.POOL_ID_PARAMETER);
        final Long imageId = HandlerUtilities.readLongParam(queryParameters, Constants.IMAGE_ID_PARAMETER);

        // query params
        final LocalDateTime validFrom = HandlerUtilities.readTimestampParam(queryParameters, Constants.VALID_FROM_PARAMETER);
        final LocalDateTime validTill = HandlerUtilities.readTimestampParam(queryParameters, Constants.VALID_TILL_PARAMETER);

        final Integer width = HandlerUtilities.readIntegerParam(queryParameters, Constants.WIDTH_PARAMETER);
        final Integer height = HandlerUtilities.readIntegerParam(queryParameters, Constants.HEIGHT_PARAMETER);

        // validations
        CheckUtilities.checkStringArgumentNotEmpty(poolId, Constants.POOL_ID_PARAMETER);
        CheckUtilities.checkPositiveArgument(imageId, Constants.IMAGE_ID_PARAMETER);

        CheckUtilities.checkArgumentNotNull(validFrom, Constants.VALID_FROM_PARAMETER);
        CheckUtilities.checkArgumentNotNull(validTill, Constants.VALID_TILL_PARAMETER);

        exchange.startBlocking();

        try (InputStream inputStream = exchange.getInputStream()) {
            final CustomImageReader reader = new CustomImageReader(inputStream);

            reader.resizeTo(width, height);

            final ImageMetadata newMetadata = new ImageMetadata(imageId, poolId, reader.getMimeType(), reader.getWidth(), reader.getHeight(), validFrom, validTill);

            if (imageUpsert(imageDatabaseManager, reader.getImageBytes(), newMetadata)) {
                exchange.setStatusCode(StatusCodes.NO_CONTENT);
            } else {
                exchange.setStatusCode(StatusCodes.CREATED);
            }

            exchange.endExchange();

        }

    }

    /**
     * @param imageBytes
     * @param metadata
     * @return true if the image was already existent
     * @throws ImageException
     */
    public static boolean imageUpsert(DatabaseManager imageDatabaseManager, final byte[] imageBytes, final ImageMetadata metadata) throws ImageException {

        final boolean[] wasImageExistent = {false};

        imageDatabaseManager.runInTransaction(connection -> {


            try {
                ImageServiceManager.removeImage(metadata.getImageId(), connection);
                wasImageExistent[0] = true;
            } catch (ImageNotFoundException ignore) {
            }

            long id = ImageServiceManager.insertPublicBinary(connection, imageBytes);

            ImageServiceManager.insertPublicImageMetadata(connection, id, metadata);

        });

        return wasImageExistent[0];
    }

}
