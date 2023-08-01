package com.valuephone.image.handler;

import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.DatabaseManager;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author tcigler
 * @since 1.0
 */
public class UserImageInfoHandler implements HttpHandler {

    private final DatabaseManager imageDatabaseManager;

    public UserImageInfoHandler(final DatabaseManager imageDatabaseManager) {
        this.imageDatabaseManager = imageDatabaseManager;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        final Long imageId = HandlerUtilities.readLongParam(exchange.getQueryParameters(), Constants.IMAGE_ID_PARAMETER);

        try (
                Connection connection = imageDatabaseManager.getConnection();
                final PreparedStatement preparedStatement = connection.prepareStatement("SELECT user_id, public FROM user_images WHERE id = ?");
        ) {
            preparedStatement.setLong(1, imageId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    exchange.getResponseSender().send(
                            resultSet.getInt(1) + ":" + resultSet.getBoolean(2)
                    );
                } else {
                    exchange.setStatusCode(StatusCodes.NOT_FOUND);
                    exchange.endExchange();
                }

            }

        }

    }

}
