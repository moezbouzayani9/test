package com.valuephone.image.task;

import com.valuephone.image.helper.ImageServiceManager;
import com.valuephone.image.utilities.DatabaseManager;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * @author tcigler
 * @since 1.0
 */
@Slf4j
public class ImageCleanupTask implements Runnable {

    private final DatabaseManager imageDatabaseManager;


    public ImageCleanupTask(final DatabaseManager imageDatabaseManager) {
        this.imageDatabaseManager = imageDatabaseManager;
    }


    @Override
    public void run() {

        try {

            imageDatabaseManager.runInDatabase(this::internalRun);
        } catch (Exception e) {

            log.error(String.format("Connection %s error => UNABLE TO CLEANUP IMAGE CACHE!", imageDatabaseManager.getIdentifier()), e);
        }
    }

    /**
     * @param connection
     * @throws SQLException
     */
    private void internalRun(Connection connection) throws SQLException {

        try (
                final PreparedStatement preparedStatement = connection.prepareStatement(
                        "WITH expired AS (" +
                                " SELECT binary_data_id, image_id" +
                                "  FROM image_metadata " +
                                "  WHERE valid_till < ? " +
                                "), deleted AS (" +
                                "   DELETE FROM binary_data " +
                                "   WHERE id IN (SELECT binary_data_id FROM expired) " +
                                "   RETURNING id" +
                                ") " +
                                " SELECT image_id FROM expired JOIN deleted ON expired.binary_data_id = deleted.id");
        ) {

            preparedStatement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                int removed = 0;
                while (resultSet.next()) {
                    ImageServiceManager.clearCachedEntries(resultSet.getLong(1));
                    removed++;
                }

                if (removed > 0) {
                    log.info("{} expired images removed from database", removed);
                } else {
                    log.debug("No expired image found");
                }
            }
        }
    }
}
