package com.valuephone.image.management.images;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * User: ahack Date: 22.09.17 Time: 09:40
 */
public class ImageRegistryImpl implements ImageRegistry {
    private static final String CACHE_BUILDER_SPEC =
            "concurrencyLevel=16,expireAfterWrite=240m,recordStats";

    private static ImageRegistryImpl instance;
    private final Cache<UUID, Long> routingTable;

    private ImageRegistryImpl() {
        routingTable = CacheBuilder.from(CACHE_BUILDER_SPEC).build();
    }

    public static synchronized ImageRegistry getInstance() {
        if (instance == null)
            instance = new ImageRegistryImpl();

        return instance;
    }

    @Override
    public UUID registerImageIdAndReturnKey(Long imageId) {
        UUID key = UUID.randomUUID();

        routingTable.put(key, imageId);

        return key;
    }

    @Override
    public Long getImageIdForKey(UUID imageKey) {
        return routingTable.getIfPresent(imageKey);
    }

    @Override
    public CacheStats getStatsForImageRegistry() {
        return routingTable.stats();
    }

    @Override
    public void clearRegistry() {
        routingTable.invalidateAll();
    }
}
