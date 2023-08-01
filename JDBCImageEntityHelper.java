package com.valuephone.image.management.images.jdbc;

import com.google.common.collect.Streams;
import com.valuephone.image.dto.ImageEntity;
import com.valuephone.image.management.share.images.ImageType;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class JDBCImageEntityHelper {

    public static final String ID = "id";
    public static final String DEDUPLICATED_IMAGE_ID = "deduplicated_image_id";
    public static final String NAME = "name";
    public static final String MODIFICATION_DATE = "modification_date";
    public static final String TEASER_IMAGE_TYPE = "teaser_image_type";
    public static final String MIME_TYPE = "mime_type";
    public static final String VALID_FROM = "valid_from";
    public static final String VALID_TILL = "valid_till";
    public static final String DELETED = "deleted";
    public static final String IMAGE_TYPE = "image_type";
    public static final String IMAGE_SIZE = "image_size";
    public static final String IMAGE_HASH = "image_hash";
    public static final String END_OF_LIFE = "end_of_life";

    public static OrderedIndexedNullableStatement setStatementParamsOrdered(PreparedStatement preparedStatement, ImageEntity imageEntity, boolean setId) throws SQLException {
        if (preparedStatement == null) {
            log.error("prepared statement can't be null");
            throw new IllegalStateException();
        }
        if (imageEntity == null) {
            log.error("image entity can't be null");
            throw new IllegalStateException("image can't be null");
        }
        OrderedIndexedNullableStatement oNStat = new OrderedIndexedNullableStatement(preparedStatement);

        if (setId) {
            oNStat.setLong(imageEntity.getId());
        }

        oNStat
            .withLong(imageEntity.getDeduplicatedImageId())
            .withString(imageEntity.getName())
            .withTimestamp(imageEntity.getModificationDate())
            .withInt(imageEntity.getTeaserImageType())
            .withString(imageEntity.getMimeType())
            .withZonedTimestamp(Optional.ofNullable(imageEntity.getValidFrom()).map(OffsetDateTime::toZonedDateTime).orElse(null))
            .withZonedTimestamp(Optional.ofNullable(imageEntity.getValidTill()).map(OffsetDateTime::toZonedDateTime).orElse(null))
            .withBoolean(imageEntity.isDeleted())
            .withString(Optional.ofNullable(imageEntity.getImageType()).map(Enum::toString).orElse(null))
            .withInt(imageEntity.getImageSize())
            .withString(imageEntity.getImageHash())
            .withDate(imageEntity.getEndOfLife());

        return oNStat;
    }

    public static String getColumnNamesOrderedJoined(boolean getId, Function<? super String, ? extends String> mappingFunction) {
        return getColumnNamesOrdered(getId).map(mappingFunction).collect(Collectors.joining(","));
    }

    private static Stream<String> getColumnNamesOrdered(boolean getId) {
        Stream<String> columnsStream = Stream.of(DEDUPLICATED_IMAGE_ID, NAME, MODIFICATION_DATE, TEASER_IMAGE_TYPE, MIME_TYPE, VALID_FROM, VALID_TILL, DELETED, IMAGE_TYPE, IMAGE_SIZE, IMAGE_HASH, END_OF_LIFE);
        if (getId) {
            return Streams.concat(Stream.of(ID), columnsStream);
        } else {
            return columnsStream;
        }
    }

    public static ImageEntity map(ResultSet resultSet) throws SQLException {
        ImageEntity image = new ImageEntity();
            image.setId(resultSet.getLong(ID));
            image.setDeduplicatedImageId(resultSet.getLong(DEDUPLICATED_IMAGE_ID));
            image.setName(resultSet.getString(NAME));
        Object modificationDate = resultSet.getObject(MODIFICATION_DATE);
        if(modificationDate != null) {
            image.setModificationDate(ImageManagerJDBCUtills.parseOffsetTimestamp(modificationDate.toString()));
        }
            image.setTeaserImageType(resultSet.getInt(TEASER_IMAGE_TYPE));
            image.setMimeType(resultSet.getString(MIME_TYPE));
                image.setValidFrom(Optional.ofNullable(resultSet.getObject(VALID_FROM))
                        .map(Object::toString).map(ImageManagerJDBCUtills::parseOffsetTimestamp).orElse(null));
            image.setValidTill(Optional.ofNullable(resultSet.getObject(VALID_TILL))
                    .map(Object::toString).map(ImageManagerJDBCUtills::parseOffsetTimestamp).orElse(null));
            image.setDeleted(resultSet.getBoolean(DELETED));
            image.setImageType(Optional.ofNullable(resultSet.getString(IMAGE_TYPE)).map(ImageType::valueOf).orElse(null));
            image.setImageSize(resultSet.getInt(IMAGE_SIZE));
            image.setImageHash(resultSet.getString(IMAGE_HASH));
            image.setEndOfLife(Optional.ofNullable(resultSet.getObject(END_OF_LIFE)).map(Object::toString).map(ImageManagerJDBCUtills::parseOffsetTimestamp).map(OffsetDateTime::toLocalDate).orElse(null));
        return image;
    }

}
