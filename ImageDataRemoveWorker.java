package com.valuephone.image.management.images.jdbc;

import lombok.extern.slf4j.Slf4j;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 21.06.16 Time: 17:07
 */
@Slf4j
public class ImageDataRemoveWorker implements Work {

    static final String QUERY_TO_DELETE_IMAGE_DATA = "DELETE FROM imagedata WHERE image_id = ?;";

    final long deduplicatedImageId;
    public boolean wasSuccessFull;

    public ImageDataRemoveWorker(long deduplicatedImageId) {
        this.deduplicatedImageId = deduplicatedImageId;
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        try (CallableStatement cs = connection.prepareCall(QUERY_TO_DELETE_IMAGE_DATA)) {
            cs.setLong(1, deduplicatedImageId);
            switch (cs.executeUpdate()) {
                case 0:
                    log.warn("binary data for deduplicatedImageId[" + deduplicatedImageId
                            + "] was not present");
                    wasSuccessFull = true;
                    break;
                case 1:
                    log.debug("binary data for deduplicatedImageId[" + deduplicatedImageId
                            + "] have been removed");
                    wasSuccessFull = true;
                    break;
                default:
                    wasSuccessFull = false;
            }
        }
    }

    public boolean wasSuccessFull() {
        return this.wasSuccessFull;
    }
}
