package com.valuephone.image.management.images.jdbc;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

@Slf4j
public class OrderedIndexedNullableStatement {

    private static final String ZONED_TIMESTAMP_DATE_FORMAT_PATTERN = "(\"yyyy-MM-dd HH:mm:ss.SSS+00\",";
    private static final String ZONED_TIMESTAMP_DATE_FORMAT_PATTERN_APPEND = ")";
    private static final SimpleDateFormat ZONED_TIMESTAMP_FORMATTER = new SimpleDateFormat(ZONED_TIMESTAMP_DATE_FORMAT_PATTERN);

    private final PreparedStatement statement;
    private int index = 1;

    public OrderedIndexedNullableStatement(PreparedStatement preparedStatement) {
        this.statement = preparedStatement;
    }

    @FunctionalInterface
    private interface StatementSetter<T> {
        void set(int parameterIndex, T val) throws SQLException;
    }

    private <T> void setStatementNullableParam(PreparedStatement statement, int index, T value, StatementSetter<T> setter, int sqlType) throws SQLException {
        if (value == null) {
            statement.setNull(index, sqlType);
        } else {
            setter.set(index, value);
        }
    }

    public OrderedIndexedNullableStatement withLong(Long value) throws SQLException {
        setLong(value);
        return this;
    }

    public void setLong(Long value) throws SQLException {
        setStatementNullableParam(statement, index++, value, statement::setLong, Types.BIGINT);
    }

    public OrderedIndexedNullableStatement withInt(Integer value) throws SQLException {
        setInt(value);
        return this;
    }

    public void setInt(Integer value) throws SQLException {
        setStatementNullableParam(statement, index++, value, statement::setInt, Types.INTEGER);
    }

    public OrderedIndexedNullableStatement withString(String value) throws SQLException {
        setString(value);
        return this;
    }

    public void setString(String value) throws SQLException {
        setStatementNullableParam(statement, index++, value, statement::setString, Types.VARCHAR);
    }

    public OrderedIndexedNullableStatement withBoolean(Boolean value) throws SQLException {
        setBoolean(value);
        return this;
    }

    public void setBoolean(Boolean value) throws SQLException {
        setStatementNullableParam(statement, index++, value, statement::setBoolean, Types.BOOLEAN);
    }

    public void setTimestamp(Timestamp value) throws SQLException {
        setStatementNullableParam(statement, index++, value, statement::setTimestamp, Types.TIMESTAMP);
    }

    public OrderedIndexedNullableStatement withTimestamp(OffsetDateTime value) throws SQLException {
        setTimestamp(value);
        return this;
    }

    public void setTimestamp(ZonedDateTime value) throws SQLException {
        setTimestamp(value != null ? Timestamp.from(value.toInstant()) : null);
    }

    public void setTimestamp(OffsetDateTime value) throws SQLException {
        setTimestamp(value != null ? Timestamp.from(value.toInstant()) : null);
    }

    public void setDate(Date value) throws SQLException {
        setStatementNullableParam(statement, index++, value, statement::setDate, Types.DATE);
    }

    public OrderedIndexedNullableStatement withDate(LocalDate value) throws SQLException {
        setDate(value);
        return this;
    }

    public void setDate(LocalDate value) throws SQLException {
        setDate(value != null ? Date.valueOf(value) : null);
    }


    public void setZonedTimestamp(ZonedDateTime value) throws SQLException {
        setStatementNullableParam(statement, index++, value == null ? null : formatZonedTimestamp(value), (i, v) -> statement.setObject(i, v, Types.OTHER), Types.OTHER);
    }

    public OrderedIndexedNullableStatement withZonedTimestamp(ZonedDateTime value) throws SQLException {
        setZonedTimestamp(value);
        return this;
    }

    public static String formatZonedTimestamp(ZonedDateTime input) {
        return ZONED_TIMESTAMP_FORMATTER.format(java.util.Date.from(input.toInstant()))
                + input.getZone().getId()
                + ZONED_TIMESTAMP_DATE_FORMAT_PATTERN_APPEND;
    }
}
