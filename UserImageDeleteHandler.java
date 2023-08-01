package com.valuephone.image.handler;

import com.valuephone.image.helper.ImageServiceManager;
import com.valuephone.image.security.ImageUserPrincipal;
import com.valuephone.image.utilities.DatabaseManager;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.StatusCodes;

import java.sql.Connection;

/**
 * @author tcigler
 * @since 1.0
 */
public class UserImageDeleteHandler extends AuthenticatedUserWrapperHandler {
    private final DatabaseManager imageDatabaseManager;

    public UserImageDeleteHandler(final DatabaseManager imageDatabaseManager) {
        this.imageDatabaseManager = imageDatabaseManager;
    }

    @Override
    protected void handleRequestAuthenticated(final HttpServerExchange exchange, final ImageUserPrincipal principal) throws Exception {

        final HeaderMap headers = exchange.getRequestHeaders();
        final HeaderValues imageIdParam = headers.get("imageId");
        if (imageIdParam == null || imageIdParam.getFirst() == null) {
            throw new IllegalArgumentException("Missing image ID to remove");
        }

        final long imageId = Long.parseLong(imageIdParam.getFirst());
        final int userId = principal.getUserId();

        try (Connection connection = imageDatabaseManager.getConnection()) {
            ImageServiceManager.removeUserImage(connection, userId, imageId);
        }

        exchange.setStatusCode(StatusCodes.NO_CONTENT);

    }

}
