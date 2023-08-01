package com.valuephone.image.management.images.jdbc;

import com.valuephone.image.management.images.ImageDeduplicationKey;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 22.06.16 Time: 13:12
 */
public class DeduplicateImageWorker implements Work {

    static final String QUERY_TO_FETCH_DEDUPLICATED_IMAGE_BY_KEY = "SELECT " +
            "min(id) " +
            "FROM image " +
            "WHERE image_size=? AND image_hash=? AND image_type=? AND NOT deleted " +
            "GROUP BY image_size, image_hash, image_type, deleted;";

    final ImageDeduplicationKey boImageDeduplicationKey;
    public long deduplicatedImageId;

    public DeduplicateImageWorker(ImageDeduplicationKey boImageDeduplicationKey) {
        this.boImageDeduplicationKey = boImageDeduplicationKey;
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        try (CallableStatement cs = connection.prepareCall(QUERY_TO_FETCH_DEDUPLICATED_IMAGE_BY_KEY)) {

            cs.setInt(1, boImageDeduplicationKey.imageSize);
            cs.setString(2, boImageDeduplicationKey.imageHash.toString());
            cs.setString(3, boImageDeduplicationKey.imageType.name());

            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next())
                    deduplicatedImageId = rs.getLong(1);
                else
                    deduplicatedImageId = 0;
            }
        }
    }
}
