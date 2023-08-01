package com.valuephone.image.management.images.jdbc;

import com.valuephone.image.dto.ImageEntity;
import com.valuephone.image.exception.ImageException;
import com.valuephone.image.exception.ImageNotFoundException;
import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.management.images.ImageLifetime;
import com.valuephone.image.management.images.ImageLifetimeKey;
import com.valuephone.image.utilities.DatabaseManager;
import com.valuephone.image.utilities.DateTimeUtilities;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.time.OffsetDateTime;

@Slf4j
public class ImageManagerJDBCUtills {

    private final DatabaseManager mhDatabaseManager;

    public ImageManagerJDBCUtills(DatabaseManager mhDatabaseManager) {
        this.mhDatabaseManager = mhDatabaseManager;
    }

    public void saveImageEntity(ImageEntity entity) throws ImageException {
        mhDatabaseManager.runInTransaction(connection -> saveImageEntity(connection, entity));
    }

    public void updateImageEntity(ImageEntity entity) throws ImageException {
        mhDatabaseManager.runInTransaction(connection -> updateImageEntity(connection, entity));
    }

    public ImageEntity getImageById(long imageEntityId) throws ImageException {
        return mhDatabaseManager.runInTransactionWithResult(c -> findImageEntity(c, imageEntityId));
    }

    public void persistImageData(ImageEntity imageEntity, byte[] imageData) throws ValuePhoneException {
        try {
            ImageDataPersistWorker imageDataPersistWorker = new ImageDataPersistWorker(imageEntity, imageData);

            mhDatabaseManager.runInTransaction(imageDataPersistWorker::execute);
            imageEntity = imageDataPersistWorker.imageEntity;
            if (imageEntity.getImageSize() == null || imageEntity.getImageHash() == null
                    || imageEntity.getMimeType() == null)
                throw new ValuePhoneException("FAILED to persistImageData(" + imageEntity.getId() + ")");

            updateImageEntity(imageEntity);
        } catch (ImageException e) {
            log.error(e.getMessage(), e);
            throw new ValuePhoneException(e, "FAILED to persistImageData(" + imageEntity.getId() + ")");
        }
    }

    public ImageLifetime getRemainingImageLifetimeForImageLifetimeKey(
            ImageLifetimeKey imageLifetimeKey, Long imageToBeRemoved)
            throws ValuePhoneException {
        try {
            RemainingImageLifetimeWorker remainingImageLifetimeWorker = new RemainingImageLifetimeWorker(
                    imageLifetimeKey, imageToBeRemoved);
            mhDatabaseManager.runInTransaction(remainingImageLifetimeWorker::execute);
            return remainingImageLifetimeWorker.imageLifetime;
        } catch (ImageException e) {
            log.error(e.getMessage(), e);
            throw new ValuePhoneException(e, "FAILED to " +
                    "getRemainingImageLifetimeForImageLifetimeKey(" + imageLifetimeKey + ")");
        }
    }

    public void removeImageDataForDeduplicatedImageId(long deduplicatedImageId) throws ValuePhoneException {
        try {
            ImageDataRemoveWorker imageDataRemoveWorker = new ImageDataRemoveWorker(deduplicatedImageId);
            mhDatabaseManager.runInTransaction(imageDataRemoveWorker::execute);
            if (!imageDataRemoveWorker.wasSuccessFull())
                throw new ValuePhoneException(
                        "FAILED to removeImageDataForDeduplicatedImageId(" + deduplicatedImageId + ")");

            log.debug("removed binary data for deduplicatedImageId[" + deduplicatedImageId + "]");
        } catch (ImageException e) {
            log.error(e.getMessage(), e);
            throw new ValuePhoneException(e,
                    "FAILED to removeImageDataForDeduplicatedImageId(" + deduplicatedImageId + ")");
        }
    }


    public byte[] getImageBytes(long imageId) {
        try {
            ImageDataByteReaderWorker imageDataByteReaderWorker = new ImageDataByteReaderWorker(imageId);
            mhDatabaseManager.runInTransaction(imageDataByteReaderWorker::execute);
            return imageDataByteReaderWorker.imageBytes;
        } catch (ImageException e) {
            log.error("FAILED to getImageBytes(" + imageId + ")", e);
            return null;
        }
    }

    private void updateImageEntity(Connection connection, ImageEntity imageEntity) throws ImageException {
        String query = "update image set " +
                JDBCImageEntityHelper.getColumnNamesOrderedJoined(false, c -> c + "=?")
                + " where id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            JDBCImageEntityHelper.setStatementParamsOrdered(preparedStatement, imageEntity, false)
                    .setLong(imageEntity.getId());


            int retVal = preparedStatement.executeUpdate();
            if (retVal != 1) {
                log.error("image could not be updated [{}]" , imageEntity);
                throw new ImageException("image could not be updated");
            }

        } catch (SQLException e) {
            log.error("image could not be updated", e);
            throw new ImageException("image could not be updated");
        }
    }

    public static OffsetDateTime parseOffsetTimestamp(String input)  {
        return DateTimeUtilities.parseOffsetTimestamp(input);
    }

    private void saveImageEntity(Connection connection, ImageEntity imageEntity) throws ImageException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("insert into image (" +
                JDBCImageEntityHelper.getColumnNamesOrderedJoined(false, c -> c)
                + ") " +
                "values (" + JDBCImageEntityHelper.getColumnNamesOrderedJoined(false, c -> "?") + ");", Statement.RETURN_GENERATED_KEYS)) {

            JDBCImageEntityHelper.setStatementParamsOrdered(preparedStatement, imageEntity, false);


            int retVal = preparedStatement.executeUpdate();
            if (retVal != 1) {
                log.error("image could not be stored [{}]", imageEntity);
                throw new ImageException("image could not be stored");
            }
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                long id = generatedKeys.getLong("id");
                imageEntity.setId(id);
            }
        } catch (SQLException e) {
            log.error("image could not be stored", e);
            throw new ImageException("image could not be stored");
        }
    }

    private ImageEntity findImageEntity(Connection connection, long imageEntityId) throws SQLException, ImageException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("select * FROM image where id = ?")) {

            preparedStatement.setLong(1, imageEntityId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return JDBCImageEntityHelper.map(resultSet);
            } else {
                log.error("image with id {} not found", imageEntityId);
                throw new ImageNotFoundException("image not found");
            }
        }
    }


    public boolean isOrphanedImageData(Long deduplicatedImageId) throws ValuePhoneException {
        try {
            ImageDataOrphanedCheckWorker imageDataOrphanedCheckWorker = new ImageDataOrphanedCheckWorker(deduplicatedImageId);
            mhDatabaseManager.runInTransaction(imageDataOrphanedCheckWorker::execute);
            return imageDataOrphanedCheckWorker.result;
        } catch (ImageException e) {
            log.error(e.getMessage(), e);
            throw new ValuePhoneException(e, "FAILED isOrphanedImageData(" + deduplicatedImageId + ")");
        }
    }
}
