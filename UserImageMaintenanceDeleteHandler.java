package com.valuephone.image.handler;

import com.valuephone.image.exception.FormatedIllegalArgumentException;
import com.valuephone.image.helper.ImageServiceManager;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.DatabaseManager;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;

import java.sql.Connection;
import java.util.Deque;
import java.util.Map;

/**
 * @author tcigler
 * @since 1.0
 */
public class UserImageMaintenanceDeleteHandler implements HttpHandler {
    private final DatabaseManager imageDatabaseManager;

    public UserImageMaintenanceDeleteHandler(final DatabaseManager imageDatabaseManager) {
        this.imageDatabaseManager = imageDatabaseManager;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        // part of the path
        final Integer userId = HandlerUtilities.readIntegerParam(queryParameters, Constants.USER_ID_PARAMETER);
        final Long imageId = HandlerUtilities.readLongParam(queryParameters, Constants.USER_IMAGE_ID_PARAMETER);

        try (Connection connection = imageDatabaseManager.getConnection()) {
            if (userId != null) {
                ImageServiceManager.removeUserImages(connection, userId);
            } else if (imageId != null) {
                ImageServiceManager.removeUserImageById(connection, imageId);
            } else {
                throw new FormatedIllegalArgumentException("%s or %s parameter should be present!", Constants.USER_ID_PARAMETER, Constants.USER_IMAGE_ID_PARAMETER);
            }

            exchange.setStatusCode(StatusCodes.NO_CONTENT);
        }

    }

}
