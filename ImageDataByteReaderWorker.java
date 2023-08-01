package com.valuephone.image.management.images.jdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 22.06.16 Time: 09:38
 */
public class ImageDataByteReaderWorker implements Work {

    static final String QUERY_TO_FETCH_IMAGE_BYTES = "SELECT " +
            "id.data " +
            "FROM image i " +
            "JOIN imagedata id ON i.deduplicated_image_id=id.image_id " +
            "WHERE i.id=?;";

    final long imageId;
    public byte[] imageBytes;

    public ImageDataByteReaderWorker(long imageId) {
        this.imageId = imageId;
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        try (CallableStatement cs = connection.prepareCall(QUERY_TO_FETCH_IMAGE_BYTES)) {
            cs.setLong(1, imageId);

            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next())
                    imageBytes = rs.getBytes(1);
                else
                    imageBytes = null;
            }
        }
    }
}
