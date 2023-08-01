package com.valuephone.image.management.images;

import com.google.common.cache.CacheStats;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 21.09.17 Time: 14:47
 */
public interface ImageRegistry {

    /**
     * Method to get a the singleton instance of the BoImageRegistry
     *
     * @return the instance of BoImageRegistry
     */
    static ImageRegistry getInstance() {
        return ImageRegistryImpl.getInstance();
    }

    /**
     * Register a route to an image ID. The maximum lifetime of this registry entry is defined by the underlying cache.
     *
     * @param imageId the ID of the image
     * @return the temporary UUID of this image
     */
    UUID registerImageIdAndReturnKey(Long imageId);

    /**
     * Method to get a image ID from the registry.
     *
     * @param imageKey the temporary UUID of an image ID
     * @return the image ID or NULL if none is registered
     */
    Long getImageIdForKey(UUID imageKey);

    /**
     * Method to get the complete stats for the underlying cache
     *
     * @return com.google.common.cache.CacheStats for the underlying cache
     */
    CacheStats getStatsForImageRegistry();

    /**
     * Method to clear the complete registry
     */
    void clearRegistry();
}
