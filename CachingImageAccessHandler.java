package com.valuephone.image.handler;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.valuephone.image.dto.OutputImageMetadata;
import com.valuephone.image.exception.ImageException;
import com.valuephone.image.exception.ImageNotFoundException;
import com.valuephone.image.helper.CustomImageReader;
import com.valuephone.image.helper.ImageManipulationManager;
import com.valuephone.image.helper.ImageServiceManager;
import com.valuephone.image.helper.ImageSize;
import com.valuephone.image.utilities.*;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.valuephone.image.utilities.HandlerUtilities.*;

/**
 * @author tcigler
 * @since 1.0
 */
@Slf4j
public class CachingImageAccessHandler implements HttpHandler {
    private static final ConcurrentMap<Path, Object> filenameLockMap = new ConcurrentHashMap<>();

    private final DatabaseManager imageDatabaseManager;

    private final LoadingCache<Long, byte[]> imageBytesCache;

    /**
     * Last accesses are not usually written to file meta, so the cache containing up-to-date information should be involved
     */
    public static final Cache<String, LocalDateTime> imageCacheLastAccesses;

    static {
        imageCacheLastAccesses = CacheBuilder.newBuilder()
                .expireAfterWrite(Constants.CACHE_IMAGE_DAYS_UNUSED_UTIL_REMOVE, TimeUnit.DAYS)
                .build();
    }

    public CachingImageAccessHandler(final DatabaseManager imageDatabaseManager) {
        this.imageDatabaseManager = imageDatabaseManager;

        // Small short-living cache for large binaries fetched in original size from database
        imageBytesCache = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .maximumSize(500)
                .build(new ImageBinaryCacheLoader(imageDatabaseManager));
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        final Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

        // parameters already checked for presence
        final Long imageId = readLongParam(queryParameters, Constants.IMAGE_ID_PARAMETER);
        final String poolId = readStringParam(queryParameters, Constants.POOL_ID_PARAMETER);

        CheckUtilities.checkStringArgumentNotEmpty(poolId, Constants.POOL_ID_PARAMETER);

        try (final Connection connection = imageDatabaseManager.getConnection()) {

            OutputImageMetadata imageMetadata = ImageServiceManager.getImageMetadata(imageId, connection);

            checkImageValidity(imageMetadata);

            if (DateTimeUtilities.isIfModifiedHeaderPresentAndActual(exchange.getRequestHeaders(), imageMetadata.getValidFrom())) {
                exchange.setStatusCode(StatusCodes.NOT_MODIFIED);
                exchange.endExchange();
                return;
            }

            CacheUtilities.addCacheControl(exchange, true, imageMetadata.getValidTill());

            final ImageSize size = new ImageSize(imageMetadata.getWidth(), imageMetadata.getHeight());
            Integer requestedWidth = readIntegerParam(queryParameters, Constants.WIDTH_PARAMETER);
            Integer requestedHeight = readIntegerParam(queryParameters, Constants.HEIGHT_PARAMETER);

            byte[] binary;

            if (ImageManipulationManager.resize(size, requestedWidth, requestedHeight)) {

                binary = getImageBytesFromFileCache(size, imageMetadata);

            } else {

                log.debug("No resize needed - returning raw image");

                binary = imageBytesCache.get(imageMetadata.getBinaryDataId());

            }

            HandlerUtilities.sendImageBytesAsResponse(exchange, imageMetadata.getMimeType(), binary);

        }
    }

    public void checkImageValidity(final OutputImageMetadata imageMetadata) throws ImageNotFoundException {
        if (!DateTimeUtilities.isPeriodValid(imageMetadata.getValidFrom(), imageMetadata.getValidTill())) {
            throw new ImageNotFoundException(imageMetadata.getImageId(), SecurityUtilities.sanitizeInputString(imageMetadata.getPoolId()));
        }
    }

    public byte[] getImageBytesFromCache(long binaryDataId) throws ExecutionException {
        return imageBytesCache.get(binaryDataId);
    }

    public byte[] getImageBytesFromFileCache(final ImageSize requestedSize, OutputImageMetadata metadata) throws ImageException, ExecutionException {

        final Path path = CacheUtilities.generatePathToCachedImage(metadata.getImageId(), requestedSize.getWidth(), requestedSize.getHeight(), metadata.getMimeType());

        final File file = path.toFile();

        if (!file.isFile()) {

            final Object newLock = new Object();

            Object lock = filenameLockMap.putIfAbsent(path, newLock);

            if (lock == null) {
                lock = newLock;
            }

            synchronized (lock) {
                if (!file.isFile()) {
                    log.info(String.format("Cached image (imageId = %d) not found - resizing (preserving ratio) to %s", metadata.getImageId(), requestedSize));

                    final byte[] binary = imageBytesCache.get(metadata.getBinaryDataId());
                    final CustomImageReader reader = new CustomImageReader(binary);

                    reader.resizeTo(requestedSize.getWidth(), requestedSize.getHeight());

                    final byte[] resizedImageBytes = reader.getImageBytes();

                    try {
                        Files.createDirectories(path.getParent());
                        Files.write(path, resizedImageBytes, StandardOpenOption.CREATE_NEW);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }

                    return resizedImageBytes;

                }
            }

            filenameLockMap.remove(path);

            return getBytesFromFile(path);
        } else {
            log.info("Successfull hit in cache");
            return getBytesFromFile(path);
        }
    }

    private byte[] getBytesFromFile(final Path path) throws ImageException {
        try {
            final String filename = path.getFileName().toString();
            imageCacheLastAccesses.put(filename, LocalDateTime.now());

            return Files.readAllBytes(path);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new ImageException("Cannot read the cached image!");
        }
    }

    private class ImageBinaryCacheLoader extends CacheLoader<Long, byte[]> {

        private final DatabaseManager imageDatabaseManager;

        ImageBinaryCacheLoader(final DatabaseManager imageDatabaseManager) {
            this.imageDatabaseManager = imageDatabaseManager;
        }

        @Override
        public byte[] load(final Long binaryDataId) throws Exception {
            try (final Connection connection = imageDatabaseManager.getConnection()) {
                return ImageServiceManager.getBinary(binaryDataId, connection);
            }
        }

    }

}
