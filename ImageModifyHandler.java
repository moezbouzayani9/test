package com.valuephone.image.handler;

import com.valuephone.image.exception.ImageNotFoundException;
import com.valuephone.image.utilities.CheckUtilities;
import com.valuephone.image.utilities.Constants;
import com.valuephone.image.utilities.DatabaseManager;
import com.valuephone.image.utilities.HandlerUtilities;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Deque;
import java.util.Map;

/**
 * @author tcigler
 * @since 1.0
 */
@Slf4j
public class ImageModifyHandler implements HttpHandler {

    private final DatabaseManager imageDatabaseManager;

    public ImageModifyHandler(final DatabaseManager imageDatabaseManager) {
        this.imageDatabaseManager = imageDatabaseManager;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        // part of the path
        final Long imageId = HandlerUtilities.readLongParam(queryParameters, Constants.IMAGE_ID_PARAMETER);

        final LocalDateTime validFrom = HandlerUtilities.readTimestampParam(queryParameters, Constants.VALID_FROM_PARAMETER);
        final LocalDateTime validTill = HandlerUtilities.readTimestampParam(queryParameters, Constants.VALID_TILL_PARAMETER);

        modifyImage(imageDatabaseManager, imageId, validFrom, validTill);

        exchange.setStatusCode(StatusCodes.NO_CONTENT);
        exchange.endExchange();

    }

    public static void modifyImage(DatabaseManager imageDatabaseManager, final Long imageId, final LocalDateTime validFrom, final LocalDateTime validTill) throws ImageNotFoundException {

        CheckUtilities.checkArgumentNotNull(validFrom, "validFrom");
        CheckUtilities.checkArgumentNotNull(validTill, "validTill");

        if (validTill.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Validity has already expired!");
        }

        if (validFrom.isAfter(validTill)) {
            throw new IllegalArgumentException("Image has negative validity range (validFrom is after validTill)");
        }

        updateMetadata(imageDatabaseManager, imageId, validFrom, validTill);

    }

    private static void updateMetadata(DatabaseManager imageDatabaseManager, final Long imageId, final LocalDateTime validFrom, final LocalDateTime validTill) throws ImageNotFoundException {
        try (
                Connection connection = imageDatabaseManager.getConnection();
                final PreparedStatement preparedStatement = connection.prepareStatement("UPDATE image_metadata SET valid_from = ?, valid_till = ? WHERE image_id = ?")
        ) {

            int x = 1;

            preparedStatement.setTimestamp(x++, Timestamp.valueOf(validFrom));
            preparedStatement.setTimestamp(x++, Timestamp.valueOf(validTill));
            preparedStatement.setLong(x, imageId);

            if (preparedStatement.executeUpdate() < 1) {
                throw new ImageNotFoundException();
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException("SQL exception");
        }
    }

}
