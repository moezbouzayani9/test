package com.valuephone.image.management.images;

import com.valuephone.image.cache.ImageDeduplicationCache;
import com.valuephone.image.exception.ValuePhoneException;
import com.valuephone.image.helper.Reject;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 18.03.16 Time: 14:38
 */
public class ImageDeduplicationManagerBean implements ImageDeduplicationManager {

    private final ImageDeduplicationCache imageDeduplicationCache;

    public ImageDeduplicationManagerBean(ImageDeduplicationCache imageDeduplicationCache) {
        this.imageDeduplicationCache = imageDeduplicationCache;
    }

    @Override
    public long getIdForImageDeduplicationKey(ImageDeduplicationKey deduplicationKey) {
        Reject.ifNull(deduplicationKey, "no deduplication key");

        return imageDeduplicationCache.getIdForDeduplicationKey(deduplicationKey);
    }

    @Override
    public void removeCachedEntryForDeduplicationKey(ImageDeduplicationKey imageDeduplicationKey) {
        Reject.ifNull(imageDeduplicationKey, "no deduplication key");

        imageDeduplicationCache.invalidateImageDeduplicationCacheForDeduplicationKey(
                imageDeduplicationKey);
    }

    @Override
    public ImageLifetime getLifetimeForImageLifetimeKey(ImageLifetimeKey imageLifetimeKey)
            throws ValuePhoneException {
        Reject.ifNull(imageLifetimeKey, "no life time key");

        return imageDeduplicationCache.getLifetimeForLifetimeKey(imageLifetimeKey);
    }

    @Override
    public ImageLifetime cacheLifetimeForImageLifetimeKeyIfAbsent(ImageLifetime imageLifetime,
                                                                  ImageLifetimeKey imageLifetimeKey) throws ValuePhoneException {
        Reject.ifNull(imageLifetime, "no image life time");
        Reject.ifNull(imageLifetimeKey, "no image life time key");

        return imageDeduplicationCache.cacheLifetimeForLifetimeKeyIfAbsent(imageLifetime, imageLifetimeKey);
    }

    @Override
    public boolean replaceLifetimeForLifetimeKey(ImageLifetime expectedImageLifetime, ImageLifetime
            newImageLifetime, ImageLifetimeKey imageLifetimeKey) throws ValuePhoneException {
        Reject.ifNull(expectedImageLifetime, "no expected image life time");
        Reject.ifNull(newImageLifetime, "no new image life time");
        Reject.ifNull(imageLifetimeKey, "no image life time key");

        return imageDeduplicationCache.replaceLifetimeForLifetimeKey(
                expectedImageLifetime, newImageLifetime, imageLifetimeKey);
    }

    @Override
    public void removeCachedEntryForImageLifetimeKey(ImageLifetimeKey imageLifetimeKey) {
        Reject.ifNull(imageLifetimeKey, "no image life time key");

        imageDeduplicationCache.invalidateImageLifetimeCacheForLifetimeKey(imageLifetimeKey);
    }
}
