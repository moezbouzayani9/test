package com.valuephone.image.management.images;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.valuephone.image.dto.ImageConfigurationEntity;
import com.valuephone.image.exception.ImageException;
import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.management.share.images.ImageConfiguration;
import com.valuephone.image.management.share.images.ImageType;
import com.valuephone.image.utilities.DatabaseManager;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CacheableImageConfigurationManagerBean implements ImageConfigurationManager {

    //    Guava does not allow null-values, so Optional is used as workaround
    LoadingCache<ImageType, Optional<ImageConfigurationEntity>> imageConfigurationCache;

    private final DatabaseManager mhDatabaseManager;

    public CacheableImageConfigurationManagerBean(DatabaseManager mhDatabaseManager) {
        this.mhDatabaseManager = mhDatabaseManager;
        imageConfigurationCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .concurrencyLevel(16)
                .build(CacheLoader.from(this::internalGetImageConfigurationEntity));
    }

    //   Fetches image configuration from the db. If it's missing, then default from ImageType is returned.
    @Override
    public synchronized ImageConfiguration getImageConfiguration(ImageType imageType) throws ValuePhoneException {
        try {
            Optional<ImageConfigurationEntity> imageConfigurationEntity = imageConfigurationCache.get(imageType);
            if (imageConfigurationEntity.isPresent()) {
                return imageConfigurationEntity.get();
            }
            log.warn("No configuration found for image type '{}'", imageType);
            return imageType;
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
            throw new ValuePhoneException(e, "Could not load image configuration for image type %s", imageType);
        }
    }

    private Optional<ImageConfigurationEntity> internalGetImageConfigurationEntity(ImageType imageType) {
        Optional<ImageConfigurationEntity> retVal;
        try {
            retVal = mhDatabaseManager.runInTransactionWithResult(connection -> {
                PreparedStatement preparedStatement = connection.prepareStatement("select * from image_configuration where image_type = ?");
                preparedStatement.setString(1, imageType.name());
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    return Optional.of(mapImageConfiguration(resultSet));
                } else {
                    return Optional.empty();
                }
            });
        } catch (ImageException e) {
            log.error("can't get image configuration");
            retVal = Optional.empty();
        }
        return retVal;
    }

    private ImageConfigurationEntity mapImageConfiguration(ResultSet resultSet) throws SQLException {
        ImageConfigurationEntity retVal = new ImageConfigurationEntity();

        retVal.setId(resultSet.getInt("id"));
        retVal.setImageType(Optional.ofNullable(resultSet.getString("image_type")).map(ImageType::valueOf).orElse(null));
        retVal.setMinWidth(resultSet.getInt("min_width"));
        retVal.setMaxWidth(resultSet.getInt("max_width"));
        retVal.setMinHeight(resultSet.getInt("min_height"));
        retVal.setMaxHeight(resultSet.getInt("max_height"));
        retVal.setMaxFileSize(resultSet.getInt("max_file_size"));

        return retVal;
    }
}
