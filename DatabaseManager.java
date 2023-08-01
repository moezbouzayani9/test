package com.valuephone.image.utilities;

import com.valuephone.image.exception.ImageException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * @author tcigler
 * @since 1.0
 */
@Slf4j
public final class DatabaseManager {

    private static final long INIT_TIMEOUT = -1L;

    private static final String JDBC_URL_TEMPLATE = "jdbc:postgresql://%s:%s/%s?sslmode=%s&autosave=always";

    private static final String INIT_SQL = "SELECT 1;";

    private static final String CHECK_SQL = "SELECT EXISTS (SELECT 1);";

    /**
     * 20 s
     */
    private static final long DEFAULT_CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(20);

    /**
     * 20 s
     */
    private static final long DEFAULT_VALIDATION_TIMEOUT = TimeUnit.SECONDS.toMillis(20);

    private static final int DEFAULT_MIN_POOL_SIZE = 10;

    private static final int DEFAULT_MAX_POOL_SIZE = 50;

    private final String identifier;

    private final DataSource dataSource;

    /**
     * @param hostName
     * @param hostPort
     * @param name
     * @param userName
     * @param userPassword
     * @param sslRequired
     * @param connectionTimeout
     * @param validationTimeout
     * @param minPoolSize
     * @param maxPoolSize
     */
    public DatabaseManager(String hostName, String hostPort, String name, String userName, String userPassword, Boolean sslRequired, Integer connectionTimeout, Integer validationTimeout, Integer minPoolSize, Integer maxPoolSize, String poolName) {

        CheckUtilities.checkStringArgumentNotEmpty(hostName, "hostName");

        CheckUtilities.checkStringArgumentNotEmpty(name, "name");

        CheckUtilities.checkStringArgumentNotEmpty(userPassword, "userPassword");

        HikariConfig config = new HikariConfig();

        String jdbcURL = String.format(JDBC_URL_TEMPLATE, hostName, hostPort, name, sslRequired != null && sslRequired ? "require" : "allow");

        log.debug("JDBC URL: {}", jdbcURL);

        config.setJdbcUrl(jdbcURL);
        config.setUsername(userName);
        config.setPassword(userPassword);

        config.setInitializationFailTimeout(INIT_TIMEOUT);
        config.setMinimumIdle(minPoolSize != null && minPoolSize > 0 ? minPoolSize : DEFAULT_MIN_POOL_SIZE);
        config.setMaximumPoolSize(maxPoolSize != null && maxPoolSize > 0 ? maxPoolSize : DEFAULT_MAX_POOL_SIZE);
        config.setConnectionInitSql(INIT_SQL);
        config.setConnectionTimeout(connectionTimeout != null && connectionTimeout >= 0 ? TimeUnit.SECONDS.toMillis(connectionTimeout) : DEFAULT_CONNECTION_TIMEOUT);
        config.setValidationTimeout(validationTimeout != null && validationTimeout >= 0 ? TimeUnit.SECONDS.toMillis(validationTimeout) : DEFAULT_VALIDATION_TIMEOUT);
        config.setRegisterMbeans(true);
        config.setPoolName(poolName);

        this.dataSource = new HikariDataSource(config);

        this.identifier = jdbcURL;
    }

    /**
     * @param jdbcURLString
     * @param dbUser
     * @param dbPassword
     */
    public DatabaseManager(String jdbcURLString, String dbUser, String dbPassword) {

        CheckUtilities.checkStringArgumentNotEmpty(jdbcURLString, "jdbcURLString");
        CheckUtilities.checkStringArgumentNotEmpty(dbUser, "dbUser");
        CheckUtilities.checkStringArgumentNotEmpty(dbPassword, "dbPassword");

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(jdbcURLString);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);

        config.setInitializationFailTimeout(INIT_TIMEOUT);
        config.setMinimumIdle(0);
        config.setConnectionInitSql(INIT_SQL);
        config.setRegisterMbeans(true);

        this.dataSource = new HikariDataSource(config);

        this.identifier = jdbcURLString;
    }

    /**
     * @return
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @return
     */
    public boolean isAlive() {

        try (Connection connection = dataSource.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(CHECK_SQL); ResultSet resultSet = preparedStatement.executeQuery()) {

            if (!resultSet.next()) {
                return false;
            }

            return resultSet.getBoolean(1);
        } catch (SQLException e) {

            log.debug(e.getMessage(), e);

            return false;
        }
    }

    /**
     * @return
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {

        try {

            Connection connection = dataSource.getConnection();

            log.trace("Connection: {}", connection);

            return connection;
        } catch (SQLException e) {

            log.error(e.getMessage(), e);

            throw e;
        }
    }

    /**
     * @param databaseRunner
     * @throws SQLException
     */
    public void runInDatabase(DatabaseRunner databaseRunner) throws SQLException {

        CheckUtilities.checkArgumentNotNull(databaseRunner, "databaseRunner");

        try (Connection connection = dataSource.getConnection()) {

            log.trace("Connection: {}", connection);

            databaseRunner.runInDatabase(connection);
        } catch (SQLException e) {

            log.error(e.getMessage(), e);

            throw e;
        }
    }

    public void runInTransaction(TransactionalExecutable execution) throws ImageException {

        try (Connection connection = getConnection()) {

            try {

                connection.setAutoCommit(false);

                execution.executeInTransaction(connection);

                connection.commit();

            } catch (Exception e) {

                connection.rollback();
                throw e;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException("SQL Exception - transaction rolled back");
        }

    }

    @FunctionalInterface
    public interface TransactionalExecutable {

        void executeInTransaction(Connection connection) throws ImageException, SQLException;

    }

    public <T> T runInTransactionWithResult(TransactionalExecutableWithResult<T> execution) throws ImageException {

        T result;

        try (Connection connection = getConnection()) {

            try {

                connection.setAutoCommit(false);

                result = execution.executeInTransaction(connection);

                connection.commit();

            } catch (Exception e) {

                connection.rollback();
                throw e;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException("SQL Exception - transaction rolled back");
        }

        return result;
    }

    @FunctionalInterface
    public interface TransactionalExecutableWithResult<T> {

        T executeInTransaction(Connection connection) throws ImageException, SQLException;

    }

}
