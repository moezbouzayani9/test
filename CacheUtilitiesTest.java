package com.valuephone.image.utilities;

import com.valuephone.image.dto.ImageMimeType;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author tcigler
 * @since 1.1
 */
class CacheUtilitiesTest {

    @Test
    void testPathToCachedFileGeneration() {

        Path path = CacheUtilities.generatePathToCachedImage(123, 10, 20, ImageMimeType.PNG);

        Path expectedPath = Paths.get("cachedImages", "32", "10", "00", "0000000123_10_20.png");

        assertEquals(expectedPath, path);

        path = CacheUtilities.generatePathToCachedImage(1234567890, 33, 44, ImageMimeType.JPG);

        expectedPath = Paths.get("cachedImages", "09", "87", "65", "1234567890_33_44.jpg");

        assertEquals(expectedPath, path);

    }

    @Test
    void testCalculateMaxAge() {

        final long maxValueAsPerHTTPStandard = 31536000L;

        final LocalDateTime date = LocalDateTime.now();

        long l = CacheUtilities.calculateMaxAge(date);
        assertEquals(0L, l);

        l = CacheUtilities.calculateMaxAge(date.minusYears(5));
        assertEquals(0L, l);

        l = CacheUtilities.calculateMaxAge(null);
        assertEquals(0L, l);

        l = CacheUtilities.calculateMaxAge(LocalDateTime.now().plusYears(5));
        assertEquals(maxValueAsPerHTTPStandard, l);

        l = CacheUtilities.calculateMaxAge(LocalDateTime.now().plusYears(1));
        assertTrue(maxValueAsPerHTTPStandard - 1 <= l); // possible one second difference

        l = CacheUtilities.calculateMaxAge(LocalDateTime.now().plusSeconds((maxValueAsPerHTTPStandard / 2)));
        assertTrue((maxValueAsPerHTTPStandard / 2) == l || (maxValueAsPerHTTPStandard / 2) - 1 == l); // possible one second difference


    }
}
