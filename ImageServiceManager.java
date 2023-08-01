package com.valuephone.image.helper;

import com.valuephone.image.dto.ImageMetadata;
import com.valuephone.image.dto.ImageMimeType;
import com.valuephone.image.dto.OutputImageMetadata;
import com.valuephone.image.exception.ImageException;
import com.valuephone.image.exception.ImageNotFoundException;
import com.valuephone.image.exception.ImageTypeNotSupportedException;
import com.valuephone.image.handler.CachingImageAccessHandler;
import com.valuephone.image.utilities.CacheUtilities;
import com.valuephone.image.utilities.TimeUtilities;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

/**
 * @author tcigler
 * @since 1.0
 */
@Slf4j
public class ImageServiceManager {

    public static final String SQL_EXCEPTION = "SQL Exception!";

    private ImageServiceManager() {
    }

    /**
     * removes image from database and all cached entries
     *
     * @param imageId    BO identifier for an image
     * @param connection db connection
     * @throws ImageNotFoundException
     */
    public static void removeImage(final long imageId, final Connection connection) throws ImageNotFoundException {

        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "DELETE FROM binary_data WHERE id IN (SELECT binary_data_id FROM image_metadata WHERE image_id = ?)")
        ) {
            preparedStatement.setLong(1, imageId);

            if (preparedStatement.executeUpdate() < 1) {
                throw new ImageNotFoundException(imageId);
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(SQL_EXCEPTION);
        }

        clearCachedEntries(imageId);

    }

    public static void removeUserImage(final Connection connection, int userId, long imageId) throws ImageNotFoundException {

        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM user_images WHERE id = ? AND user_id = ?")) {

            preparedStatement.setLong(1, imageId);
            preparedStatement.setInt(2, userId);

            final int deleted = preparedStatement.executeUpdate();

            if (deleted == 0) {
                throw new ImageNotFoundException("Image %d for user id %d was not found", imageId, userId);
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(SQL_EXCEPTION);
        }
    }

    public static void removeUserImages(final Connection connection, int userId) throws ImageNotFoundException {

        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM user_images WHERE user_id = ?")) {

            preparedStatement.setInt(1, userId);

            final int deleted = preparedStatement.executeUpdate();

            if (deleted == 0) {
                throw new ImageNotFoundException("No image user id %d was found", userId);
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(SQL_EXCEPTION);
        }
    }

    public static void removeUserImageById(final Connection connection, long id) throws ImageNotFoundException {

        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM user_images WHERE id = ?")) {

            preparedStatement.setLong(1, id);

            final int deleted = preparedStatement.executeUpdate();

            if (deleted == 0) {
                throw new ImageNotFoundException("User image with id %d was not found", id);
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(SQL_EXCEPTION);
        }
    }

    public static void clearCachedEntries(final Long imageId) {
        // TODO: make it better than dummy way
        final Path path = CacheUtilities.generatePathToCachedImage(imageId, 1, 1, ImageMimeType.JPG);

        final Path baseDir = path.getParent();

        if (baseDir.toFile().isDirectory()) {

            String filenameGlob = path.getFileName().toString();
            filenameGlob = filenameGlob.substring(0, filenameGlob.indexOf('_')) + "*";

            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(baseDir, filenameGlob)) {

                for (Path file : dirStream) {
                    Files.delete(file);

                    final String filename = file.getFileName().toString();

                    CachingImageAccessHandler.imageCacheLastAccesses.invalidate(filename);
                }

            } catch (IOException ignore) {
                log.error(ignore.getMessage(), ignore);
            }

        }
    }

    /**
     * Fetches and returns metadata for image identified by imageId (pool is not taken into account)
     *
     * @param imageId    BO identifier for an image
     * @param connection db connection
     * @return valid metadata
     * @throws ImageNotFoundException when image is not present in the DB
     */
    public static OutputImageMetadata getImageMetadata(final long imageId, final Connection connection) throws ImageNotFoundException {

        try (final PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT id, binary_data_id, image_id, mime_type, width, height, valid_from, valid_till, image_pool_id " +
                        "FROM image_metadata WHERE image_id = ?")) {

            preparedStatement.setLong(1, imageId);

            try (final ResultSet resultSet = preparedStatement.executeQuery()) {

                if (!resultSet.next()) {
                    throw new ImageNotFoundException();
                }

                return new OutputImageMetadata(
                        resultSet.getLong("id"),
                        resultSet.getLong("binary_data_id"),
                        resultSet.getLong("image_id"),
                        ImageMimeType.fromString(resultSet.getString("mime_type")),
                        resultSet.getInt("width"),
                        resultSet.getInt("height"),
                        resultSet.getString("image_pool_id"),
                        TimeUtilities.convertFromDateToLocalDateTime(resultSet.getTimestamp("valid_from")),
                        TimeUtilities.convertFromDateToLocalDateTime(resultSet.getTimestamp("valid_till"))
                );

            } catch (ImageTypeNotSupportedException e) {
                log.error(e.getMessage(), e);
                throw new IllegalStateException("Image mime type in database is not supported for showing");
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(SQL_EXCEPTION);
        }

    }

    public static byte[] getBinary(final long binaryDataId, final Connection connection) {

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT content FROM binary_data WHERE id=?")) {
            preparedStatement.setLong(1, binaryDataId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {

                if (resultSet.next()) {
                    return resultSet.getBytes(1);
                } else {
                    throw new IllegalStateException("Binary data (ID=" + binaryDataId + ") was not found for valid metadata!");
                }

            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(SQL_EXCEPTION);
        }

    }

    public static void insertPublicImageMetadata(final Connection connection, final long binaryId, final ImageMetadata metadata) throws SQLException {
        try (PreparedStatement metadataStatement = connection.prepareStatement(
                "INSERT INTO image_metadata (binary_data_id, image_id, mime_type, width, height, valid_from, valid_till, image_pool_id) " +
                        " VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {

            int x = 1;

            metadataStatement.setLong(x++, binaryId);
            metadataStatement.setLong(x++, metadata.getImageId());
            metadataStatement.setString(x++, metadata.getMimeType().toString());
            metadataStatement.setInt(x++, metadata.getWidth());
            metadataStatement.setInt(x++, metadata.getHeight());
            metadataStatement.setTimestamp(x++, Timestamp.valueOf(metadata.getValidFrom()));
            metadataStatement.setTimestamp(x++, Timestamp.valueOf(metadata.getValidTill()));
            metadataStatement.setString(x, metadata.getPoolId());

            metadataStatement.execute();

        }
    }

    public static long insertPublicBinary(final Connection connection, final byte[] imageBytes) throws SQLException, ImageException {
        final long id;
        try (PreparedStatement binaryStatement = connection.prepareStatement(
                "INSERT INTO binary_data (content) VALUES (?) RETURNING id")) {

            binaryStatement.setBytes(1, imageBytes);

            try (ResultSet returning = binaryStatement.executeQuery()) {
                if (!returning.next()) {
                    throw new ImageException("Cannot store image");
                }
                id = returning.getLong("id");
            }

        }
        return id;
    }

}
