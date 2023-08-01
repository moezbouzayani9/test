package com.valuephone.image.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.management.images.*;
import com.valuephone.image.management.images.jdbc.DeduplicateImageWorker;
import com.valuephone.image.management.images.jdbc.ImageLifetimeWorker;
import com.valuephone.image.utilities.DatabaseManager;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 22.03.16 Time: 10:29
 */
@Slf4j
public class ImageDeduplicationCacheBean implements ImageDeduplicationCache {

    private static final String CACHE_BUILDER_SPEC_FOR_BO_IMAGE_CACHE =
            "concurrencyLevel=16,expireAfterAccess=120m,recordStats";

    private LoadingCache<ImageDeduplicationKey, Long> imageDeduplicationCache;
    private LoadingCache<ImageLifetimeKey, ImageLifetime> imageLifetimeCache;

    private final DatabaseManager mhDatabaseManager;


    public ImageDeduplicationCacheBean(DatabaseManager mhDatabaseManager) {
        this.mhDatabaseManager = mhDatabaseManager;

        initializeBoImageDeduplicationCache();
        initializeBoImageLifetimeCache();
    }

    private void initializeBoImageDeduplicationCache() {
        try {
            imageDeduplicationCache = CacheBuilder
                    .from(CACHE_BUILDER_SPEC_FOR_BO_IMAGE_CACHE)
                    .build(new CacheLoader<ImageDeduplicationKey, Long>() {
                        @Override
                        public Long load(ImageDeduplicationKey imageDeduplicationKey)
                                throws Exception {
                            return loadDuplicatedImageIdForDeduplicationKey(imageDeduplicationKey);
                        }
                    }
                    );

        } catch (Throwable t) {
            log.error("FAILED to initialize boImageDeduplicationCache", t);
        }
    }

    private Long loadDuplicatedImageIdForDeduplicationKey(ImageDeduplicationKey deduplicationKey) throws
            ValuePhoneException {
        DeduplicateImageWorker deduplicateImageWorker = new DeduplicateImageWorker(deduplicationKey);
        mhDatabaseManager.runInTransaction(deduplicateImageWorker::execute);

        if (deduplicateImageWorker.deduplicatedImageId > 0) {
            return deduplicateImageWorker.deduplicatedImageId;
        }
        else {
            throw new ValuePhoneException(
                    "This exception is only to satisfy the loading cache. " +
                            "It will be handled in the reading methods");
        }
    }

    private void initializeBoImageLifetimeCache() {
        try {
            imageLifetimeCache = CacheBuilder
                    .from(CACHE_BUILDER_SPEC_FOR_BO_IMAGE_CACHE)
                    .build(new CacheLoader<ImageLifetimeKey, ImageLifetime>() {
                        @Override
                        public ImageLifetime load(ImageLifetimeKey imageLifetimeKey)
                                throws Exception {
                            return loadBoImageLifetimeForKey(imageLifetimeKey);
                        }
                    }
                    );

        } catch (Throwable t) {
            log.error("FAILED to initialize boImageDeduplicationCache", t);
        }
    }

    private ImageLifetime loadBoImageLifetimeForKey(ImageLifetimeKey imageLifetimeKey)
            throws ValuePhoneException {
        ImageLifetimeWorker imageLifetimeWorker = new ImageLifetimeWorker(
                imageLifetimeKey);
        mhDatabaseManager.runInTransaction(imageLifetimeWorker::execute);
        return imageLifetimeWorker.imageLifetime;
    }

    @Override
    public synchronized long getIdForDeduplicationKey(ImageDeduplicationKey deduplicationKey) {
        try {
            return imageDeduplicationCache.get(deduplicationKey);
        } catch (ExecutionException e) {
            return 0;
        }
    }

    @Override
    public synchronized ImageLifetime getLifetimeForLifetimeKey(ImageLifetimeKey imageLifetimeKey) throws ValuePhoneException {
        try {
            ImageLifetime res = imageLifetimeCache.get(imageLifetimeKey);
            if (res.isOutdatedLifetime()) {
                imageLifetimeCache.invalidate(imageLifetimeKey);
                return new MissingImageLifetime();
            } else {
                return res;
            }
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
            throw new ValuePhoneException("FAILED to getLifetimeForLifetimeKey[" + imageLifetimeKey + "]");
        }
    }

    @Override
    public synchronized ImageLifetime cacheLifetimeForLifetimeKeyIfAbsent(ImageLifetime imageLifetime, ImageLifetimeKey
            imageLifetimeKey) throws ValuePhoneException {
        ImageLifetime currentLifetime = getLifetimeForLifetimeKey(imageLifetimeKey);
        if (!(currentLifetime.isMissingLifetime || currentLifetime.isOutdatedLifetime())) {
            return currentLifetime;
        }

        imageLifetimeCache.put(imageLifetimeKey, imageLifetime);
        return null;
    }

    @Override
    public synchronized boolean replaceLifetimeForLifetimeKey(ImageLifetime expectedImageLifetime, ImageLifetime
            newImageLifetime, ImageLifetimeKey imageLifetimeKey) throws ValuePhoneException {
        ImageLifetime currentLifetime = getLifetimeForLifetimeKey(imageLifetimeKey);
        if (currentLifetime.isMissingLifetime || currentLifetime.isOutdatedLifetime()) {
            return false;
        }

        if (!currentLifetime.equals(expectedImageLifetime)) {
            return false;
        }

        imageLifetimeCache.put(imageLifetimeKey, newImageLifetime);
        return true;
    }

    @Override
    public synchronized void invalidateImageDeduplicationCacheForDeduplicationKey(ImageDeduplicationKey deduplicationKey) {
        Long deduplicatedImageId = getIdForDeduplicationKey(deduplicationKey);
        imageDeduplicationCache.invalidate(deduplicationKey);
        imageLifetimeCache.invalidate(ImageDeduplicationManager
                .createAndReturnLifetimeKeyForDeduplicatedImageIdAndType(
                        deduplicatedImageId));
    }

    @Override
    public synchronized void invalidateImageLifetimeCacheForLifetimeKey(ImageLifetimeKey imageLifetimeKey) {
        imageLifetimeCache.invalidate(imageLifetimeKey);
    }

    @Override
    public CacheStats getStatsForImageDeduplicationCache() {
        return imageDeduplicationCache.stats();
    }

    @Override
    public CacheStats getStatsForImageLifetimeCache() {
        return imageLifetimeCache.stats();
    }

    @Override
    public void clearCache() {
        imageDeduplicationCache.invalidateAll();
        imageLifetimeCache.invalidateAll();
    }
}
