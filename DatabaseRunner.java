package com.valuephone.image.utilities;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author lbalint
 * @since 1.0
 */
@FunctionalInterface
public interface DatabaseRunner {

    /**
     * @param connection
     * @throws SQLException
     */
    void runInDatabase(Connection connection) throws SQLException;

}
