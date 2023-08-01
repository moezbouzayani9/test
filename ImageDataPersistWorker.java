package com.valuephone.image.management.images.jdbc;

import com.valuephone.image.dto.ImageEntity;
import com.valuephone.image.management.images.ImageDeduplicationManager;
import com.valuephone.image.utilities.SimpleImageInfoUtilities;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 21.06.16 Time: 14:11
 */
@Slf4j
public class ImageDataPersistWorker implements Work {

    private static final String DEFAULT_MIME_TYPE = "image/png";

    static final String QUERY_TO_DELETE_OLD_IMAGE_DATA =
            "DELETE FROM imagedata WHERE image_id=?;";

    static final String QUERY_TO_INSERT_NEW_IMAGE_DATA =
            "INSERT INTO imagedata(image_id, data) VALUES (?, ?);";

    public final ImageEntity imageEntity;
    final byte[] imageBytes;

    public ImageDataPersistWorker(ImageEntity imageEntity, byte[] imageBytes) {
        this.imageEntity = imageEntity;
        this.imageBytes = imageBytes;
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        try (CallableStatement cs = connection.prepareCall(QUERY_TO_DELETE_OLD_IMAGE_DATA)) {
            cs.setLong(1, imageEntity.getId());
            cs.execute();
        }

        try (CallableStatement cs = connection.prepareCall(QUERY_TO_INSERT_NEW_IMAGE_DATA)) {
            cs.setLong(1, imageEntity.getId());
            cs.setBytes(2, imageBytes);
            cs.execute();
        }

        try {
            imageEntity.setMimeType(SimpleImageInfoUtilities.getImageInfo(imageBytes).getMimeType());
        } catch (IOException e) {
            imageEntity.setMimeType(DEFAULT_MIME_TYPE);
            log.warn("failed to detect mimeType by imageData for image[" +
                    imageEntity.getId() + "] -> using default mime type", e);
        }
        imageEntity.setImageSize(imageBytes.length);
        imageEntity.setImageHash(ImageDeduplicationManager
                .createAndReturnHashCodeForImageData(imageBytes).toString());
    }
}
