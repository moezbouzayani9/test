package com.valuephone.image.management.images.jdbc;

import com.valuephone.image.management.images.ImageLifetime;
import com.valuephone.image.management.images.ImageLifetimeKey;
import com.valuephone.image.management.images.MissingImageLifetime;
import org.postgresql.util.PGTimestamp;

import java.sql.*;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Optional;
import java.util.TimeZone;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 21.06.16 Time: 14:10
 */
public class ImageLifetimeWorker implements Work {

    static final String QUERY_TO_FETCH_LIFETIME = "SELECT min((valid_from).date_and_time), " +
            "       max((valid_till).date_and_time) " +
            "FROM image " +
            "WHERE deduplicated_image_id=? " +
            "  AND NOT deleted;";

    final ImageLifetimeKey imageLifetimeKey;
    public ImageLifetime imageLifetime;

    public ImageLifetimeWorker(ImageLifetimeKey imageLifetimeKey) {
        this.imageLifetimeKey = imageLifetimeKey;
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        try (CallableStatement cs = connection.prepareCall(QUERY_TO_FETCH_LIFETIME)) {

            cs.setLong(1, imageLifetimeKey.deduplicatedImageId);

            try (ResultSet rs = cs.executeQuery()) {
                fetchLifetimeFromResultSet(rs);
            }
        }
    }

    void fetchLifetimeFromResultSet(ResultSet rs) throws SQLException {
        if (rs.next()) {
            Timestamp validFrom = rs.getTimestamp(1);
            Timestamp validTill = rs.getTimestamp(2);

            ZoneId validFromZoneId = Optional.ofNullable(validFrom)
                    .filter(d -> d instanceof PGTimestamp)
                    .map(d -> (PGTimestamp) d)
                    .map(PGTimestamp::getCalendar)
                    .map(Calendar::getTimeZone)
                    .map(TimeZone::toZoneId)
                    .orElse(ZoneId.systemDefault());
            ZoneId validTillZoneId = Optional.ofNullable(validTill)
                    .filter(d -> d instanceof PGTimestamp)
                    .map(d -> (PGTimestamp) d)
                    .map(PGTimestamp::getCalendar)
                    .map(Calendar::getTimeZone)
                    .map(TimeZone::toZoneId)
                    .orElse(ZoneId.systemDefault());

            if (validFrom != null || validTill != null)
                imageLifetime = new ImageLifetime(validFrom, validFromZoneId, validTill, validTillZoneId);
            else
                imageLifetime = new MissingImageLifetime();
        } else
            throw new SQLException("min()/max() aggregates without an result ?");
    }
}
