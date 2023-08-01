package com.valuephone.image.helper;

import com.valuephone.image.dto.ImageMimeType;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author tcigler
 * @since 1.0
 */
class CustomImageReaderTest {

    private final Path imagesFolder = Paths.get("src/test/resources", "testImages");

    @Test
    void tryReadBrokenImages() throws IOException {

        Files.walkFileTree(imagesFolder, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try (final InputStream inputStream = Files.newInputStream(file)) {
                    final CustomImageReader imageReader = new CustomImageReader(inputStream);

                    final ImageMimeType mimeType = imageReader.getMimeType();
                    final String extension = mimeType.getExtension();

                    // FIXME: only valid for actual set of test images!
                    assertTrue(file.getFileName().toString().endsWith(extension));

                    final BufferedImage bufferedImage = imageReader.getBufferedImage();

                    assertNotNull(bufferedImage);

                } catch (Exception e) {
                    fail(e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }
        });

    }
}
