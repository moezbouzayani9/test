package com.valuephone.image.management.images.jdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 21.06.16 Time: 16:23
 */
public class ImageDataOrphanedCheckWorker implements Work {

    static final String QUERY_TO_COUNT_LIVING_REFERENCES = "SELECT EXISTS(" +
            "SELECT 1 " +
            "FROM image " +
            "WHERE deduplicated_image_id=? AND NOT deleted);";

    final long deduplicatedImageId;
    public boolean result;

    public ImageDataOrphanedCheckWorker(long deduplicatedImageId) {
        this.deduplicatedImageId = deduplicatedImageId;
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        try (CallableStatement cs = connection.prepareCall(QUERY_TO_COUNT_LIVING_REFERENCES)) {
            cs.setLong(1, deduplicatedImageId);

            try (ResultSet rs = cs.executeQuery()) {
                rs.next();
                result = !rs.getBoolean(1);
            }
        }
    }
}
