package com.valuephone.image.task;

import com.valuephone.image.handler.CachingImageAccessHandler;
import com.valuephone.image.utilities.Constants;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * @author tcigler
 * @since 1.0
 */
@Slf4j
public class CacheCleanupTask implements Runnable {

    @Override
    public void run() {

        final Path cachedImagesPath = Paths.get("cachedImages");

        if (!cachedImagesPath.toFile().isDirectory()) {
            return;
        }

        try {

            Files.walkFileTree(cachedImagesPath, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {

                    final String lastAccessCacheKey = file.getFileName().toString();

                    final LocalDateTime lastAccessDateTime =
                            Optional.ofNullable(CachingImageAccessHandler.imageCacheLastAccesses.getIfPresent(lastAccessCacheKey))
                                    .orElseGet(() -> LocalDateTime.ofInstant(attrs.lastAccessTime().toInstant(), ZoneId.systemDefault()));

                    LocalDateTime dateTimeThreshold = LocalDateTime.now().minusDays(Constants.CACHE_IMAGE_DAYS_UNUSED_UTIL_REMOVE);

                    if (lastAccessDateTime.isBefore(dateTimeThreshold)) {
                        Files.delete(file);
                        CachingImageAccessHandler.imageCacheLastAccesses.invalidate(lastAccessCacheKey);
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
                    log.warn(exc.getMessage(), exc);

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {

                    if (exc == null && !cachedImagesPath.equals(dir)) { // do not remove root dir
                        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
                            if (!dirStream.iterator().hasNext()) {
                                Files.delete(dir);
                            }
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            log.warn(e.getMessage(), e);
        }

    }

}
