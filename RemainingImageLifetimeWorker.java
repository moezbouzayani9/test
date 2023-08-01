package com.valuephone.image.management.images.jdbc;

import com.valuephone.image.management.images.ImageLifetimeKey;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 19.09.16 Time: 17:02
 */
public class RemainingImageLifetimeWorker extends ImageLifetimeWorker {

    static final String QUERY_TO_FETCH_REMAINING_LIFETIME = "SELECT min((valid_from).date_and_time), " +
            "       max((valid_till).date_and_time) " +
            "FROM image " +
            "WHERE deduplicated_image_id=? " +
            "  AND NOT deleted AND id!=?;";

    final Long imageToBeRemoved;

    public RemainingImageLifetimeWorker(ImageLifetimeKey imageLifetimeKey, Long imageToBeRemoved) {
        super(imageLifetimeKey);
        this.imageToBeRemoved = imageToBeRemoved;
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        try (CallableStatement cs = connection.prepareCall(QUERY_TO_FETCH_REMAINING_LIFETIME)) {

            cs.setLong(1, imageLifetimeKey.deduplicatedImageId);
            cs.setLong(2, imageToBeRemoved);

            try (ResultSet rs = cs.executeQuery()) {
                fetchLifetimeFromResultSet(rs);
            }
        }
    }
}
