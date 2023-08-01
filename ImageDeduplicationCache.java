package com.valuephone.image.cache;

import com.google.common.cache.CacheStats;
import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.management.images.ImageDeduplicationKey;
import com.valuephone.image.management.images.ImageLifetime;
import com.valuephone.image.management.images.ImageLifetimeKey;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 22.03.16 Time: 08:41
 */
public interface ImageDeduplicationCache {

    /**
     * method to return the ID of the duplicated (the original) image if one exists
     *
     * @param deduplicationKey
     *            the DeduplicationKey of the image
     * @return the ID of the duplicated image if exists or 0 if non exists
     */
    long getIdForDeduplicationKey(ImageDeduplicationKey deduplicationKey);

    /**
     * method to return the lifetime of the duplicated (the original) image if one exists
     *
     * @param imageLifetimeKey
     *            the LifetimeKey of the image
     * @return the lifetime of the duplicated image
     */
    ImageLifetime getLifetimeForLifetimeKey(ImageLifetimeKey imageLifetimeKey) throws ValuePhoneException;

    /**
     * method to manually cache an duplicated (the original) images lifetime
     *
     * @param imageLifetime
     *            the lifetime of the duplicated image
     * @param imageLifetimeKey
     *            the LifetimeKey of the image
     * @return the previous value associated with the specified boImageLifetimeKey, or {@code null} if there was no
     *         mapping for the key
     */
    ImageLifetime cacheLifetimeForLifetimeKeyIfAbsent(
            ImageLifetime imageLifetime, ImageLifetimeKey imageLifetimeKey) throws ValuePhoneException;

    /**
     * method to manually replace an duplicated (the original) images lifetime
     *
     * @param expectedImageLifetime
     *            the expected currently cached lifetime of the duplicated image
     * @param newImageLifetime
     *            the new to be cached lifetime of the duplicated image
     * @param imageLifetimeKey
     *            the LifetimeKey of the image
     * @return {@code true} if currently cached value matched expected cached value {@code false} if currently cached
     *         value did not match expected cached value
     */
    boolean replaceLifetimeForLifetimeKey(
            ImageLifetime expectedImageLifetime, ImageLifetime newImageLifetime,
            ImageLifetimeKey imageLifetimeKey) throws ValuePhoneException;

    void invalidateImageDeduplicationCacheForDeduplicationKey(ImageDeduplicationKey deduplicationKey);

    void invalidateImageLifetimeCacheForLifetimeKey(ImageLifetimeKey imageLifetimeKey);

    CacheStats getStatsForImageDeduplicationCache();

    CacheStats getStatsForImageLifetimeCache();

    void clearCache();
}
