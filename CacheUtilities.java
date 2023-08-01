package com.valuephone.image.utilities;

import com.valuephone.image.dto.ImageMimeType;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * @author tcigler
 * @since 1.0
 */
public class CacheUtilities {

    private CacheUtilities() {
    }

    /**
     * Add maximum possible value of max age to cache control (one year)
     *
     * @param exchange
     */
    public static void addCacheControlMaxAge(HttpServerExchange exchange, boolean isPublicContent) {
        addCacheControl(exchange, isPublicContent, LocalDateTime.now().plusYears(1));
    }

    /**
     * Add max age to cache control based on validTill date - maximum one year
     *
     * @param exchange
     * @param validTill
     */
    public static void addCacheControl(HttpServerExchange exchange, boolean isPublicContent, LocalDateTime validTill) {
        final HeaderMap headers = exchange.getResponseHeaders();
        headers.put(Headers.CACHE_CONTROL, getCacheControlString(isPublicContent, validTill));
    }

    public static void denyCaching(HttpServerExchange exchange, boolean isPublicContent) {
        final HeaderMap headers = exchange.getResponseHeaders();
        headers.put(Headers.CACHE_CONTROL, getCacheControlString(isPublicContent, null));
    }

    /**
     * From first X letters from left-zero-padded and reversed imageId creates folder structure
     * (X is defined by folderNameLenght and numberOfSubFolders inside method body)
     * <p>
     * Example: for id=123, width=10, height=20, mimeType=JPG; we'll get following path
     * cachedImages/32/10/00/0000000123_10_20.jpg
     *
     * @return path to (existent or potentially new) cache file
     */
    public static Path generatePathToCachedImage(long imageId, int width, int height, ImageMimeType mimeType) {

        final String originalZeroPadded = String.format("%010d", imageId);

        final String reversed = new StringBuilder(originalZeroPadded).reverse().toString();

        final int folderNameLenght = 2;
        final int numberOfSubFolders = 3;

        int substringOffset = 0;

        String[] pathParts = new String[numberOfSubFolders + 1];

        for (int i = 0; i < numberOfSubFolders; i++) {
            pathParts[i] = reversed.substring(substringOffset, substringOffset + folderNameLenght);
            substringOffset += folderNameLenght;
        }

        pathParts[numberOfSubFolders] = String.format("%s_%d_%d.%s", originalZeroPadded, width, height, mimeType.getExtension());

        return Paths.get("cachedImages", pathParts);

    }

    /**
     * @param isPublic
     * @param validTill if null, max-age = 0, otherwise fill in seconds from now to this date up to one year maximum
     * @return
     */
    private static String getCacheControlString(boolean isPublic, LocalDateTime validTill) {
        StringBuilder builder = new StringBuilder();

        if (isPublic) {
            builder.append("public, ");
        } else {
            builder.append("private, ");
        }

        builder.append("max-age=").append(calculateMaxAge(validTill));

        return builder.toString();
    }

    static long calculateMaxAge(LocalDateTime validTill) {
        final int secondsPerDay = 60 * 60 * 24;

        final long httpMaxAgeMaxValue = secondsPerDay * 365L; //as per http1.1 standard

        LocalDateTime now = LocalDateTime.now();

        if (validTill == null || now.isAfter(validTill)) {
            return 0L;
        }

        long validTillAge = ChronoUnit.SECONDS.between(now, validTill);

        return Math.min(httpMaxAgeMaxValue, validTillAge);
    }
}
