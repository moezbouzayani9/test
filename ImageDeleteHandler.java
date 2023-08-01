package com.valuephone.image.handler;

import com.valuephone.image.exception.ImageNotFoundException;
import com.valuephone.image.helper.ImageServiceManager;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.DatabaseManager;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Deque;
import java.util.Map;

/**
 * @author tcigler
 * @since 1.0
 */
@Slf4j
public class ImageDeleteHandler implements HttpHandler {

    private DatabaseManager imageDatabaseManager;

    public ImageDeleteHandler(final DatabaseManager imageDatabaseManager) {
        this.imageDatabaseManager = imageDatabaseManager;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        // part of the path
        final String poolId = HandlerUtilities.readStringParam(queryParameters, Constants.POOL_ID_PARAMETER);
        final Long imageId = HandlerUtilities.readLongParam(queryParameters, Constants.IMAGE_ID_PARAMETER);

        deleteImage(imageId, poolId);

        exchange.setStatusCode(StatusCodes.NO_CONTENT);
        exchange.endExchange();

    }

    /**
     * @param imageId BO identifier of an image
     * @param poolId  just for logging, otherwise it's ignored (imageId is unique on BO)
     * @throws ImageNotFoundException
     */
    private void deleteImage(final long imageId, final String poolId) throws ImageNotFoundException {

        log.debug("Removing image id={} pool={}", imageId, poolId);

        try (
                Connection connection = imageDatabaseManager.getConnection();
        ) {

            ImageServiceManager.removeImage(imageId, connection);

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException("SQL exception");
        }

    }

}
