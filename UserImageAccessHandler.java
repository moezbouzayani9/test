package com.valuephone.image.handler;

import com.valuephone.image.dto.ImageMimeType;
import com.valuephone.image.dto.OutputUserImage;
import com.valuephone.image.exception.ImageNotFoundException;
import com.valuephone.image.exception.ImageTypeNotSupportedException;
import com.valuephone.image.security.ImageUserPrincipal;
import com.valuephone.image.utilities.CacheUtilities;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.DatabaseManager;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Deque;
import java.util.Map;

import static com.valuephone.image.utilities.HandlerUtilities.readIntegerParam;
import static com.valuephone.image.utilities.HandlerUtilities.readLongParam;

/**
 * @author tcigler
 * @since 1.0
 */
@Slf4j
public class UserImageAccessHandler extends AuthenticatedUserWrapperHandler {

    private final DatabaseManager imageDatabaseManager;

    public UserImageAccessHandler(final DatabaseManager imageDatabaseManager) {
        this.imageDatabaseManager = imageDatabaseManager;
    }

    @Override
    public void handleRequestAuthenticated(final HttpServerExchange exchange, ImageUserPrincipal principal) throws Exception {

        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        final Long imageId = readLongParam(queryParameters, Constants.IMAGE_ID_PARAMETER);

        try (Connection connection = imageDatabaseManager.getConnection()) {

            final OutputUserImage userImage = getUserImage(imageId, principal, connection);

            Integer requestedWidth = readIntegerParam(queryParameters, Constants.WIDTH_PARAMETER);
            Integer requestedHeight = readIntegerParam(queryParameters, Constants.HEIGHT_PARAMETER);

            CacheUtilities.addCacheControlMaxAge(exchange, userImage.isPubliclyVisible());

            HandlerUtilities.sendImageAsResponse(exchange, userImage.getImageBytes(), userImage.getMimeType(), requestedWidth, requestedHeight);

        }

    }


    public static OutputUserImage getUserImage(final long imageId, ImageUserPrincipal principal, final Connection connection) throws ImageNotFoundException {

        try (final PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT mime_type, content, public " +
                        " FROM user_images WHERE id = ? AND (public OR user_id = ?)")
        ) {

            preparedStatement.setLong(1, imageId);
            preparedStatement.setInt(2, principal.getUserId());

            try (final ResultSet resultSet = preparedStatement.executeQuery()) {

                if (!resultSet.next()) {
                    throw new ImageNotFoundException(imageId, Constants.USER_IMAGE_POOL);
                }

                final ImageMimeType mimeType = ImageMimeType.fromString(resultSet.getString("mime_type"));
                final byte[] content = resultSet.getBytes("content");
                final boolean isPublic = resultSet.getBoolean("public");

                return new OutputUserImage(content, mimeType, isPublic);

            } catch (ImageTypeNotSupportedException e) {
                log.error(e.getMessage(), e);
                throw new IllegalStateException("Image mime type in database is not supported for showing");
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException("SQL Exception!");
        }

    }


}
