package com.valuephone.image.management.images.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public interface Work {
    void execute(Connection connection) throws SQLException;
}
