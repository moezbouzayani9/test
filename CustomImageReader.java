package com.valuephone.image.helper;


import com.valuephone.image.dto.ImageMimeType;
import com.valuephone.image.exception.ImageException;
import com.valuephone.image.exception.ImageTypeNotSupportedException;
import com.valuephone.image.utilities.SecurityUtilities;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Class CustomImageReader
 *
 * @author Tomas Cigler
 * @since 1.0
 */
@Slf4j
public class CustomImageReader extends ImageManipulationBase {

    public CustomImageReader(byte[] imageBytes)
            throws ImageException {
        this(readInputImageBytes(imageBytes));

        this.imageBytes = imageBytes;
    }

    public CustomImageReader(InputStream imageIS)
            throws ImageException {
        this(readInputImageStream(imageIS));
    }

    private CustomImageReader(final ValuePair<BufferedImage, ImageMimeType> imageMimeTypePair) {
        super(imageMimeTypePair.getFirstValue(), imageMimeTypePair.getSecondValue());
    }

    private static ValuePair<BufferedImage, ImageMimeType> readInputImageStream(final InputStream imageIS) throws ImageException {
        try (MemoryCacheImageInputStream memCache = new MemoryCacheImageInputStream(imageIS)) {
            return readImageFromStream(memCache);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new ImageException("Cannot read image stream");
        }
    }


    private static ValuePair<BufferedImage, ImageMimeType> readInputImageBytes(final byte[] imageBytes) throws ImageException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
             MemoryCacheImageInputStream memCache = new MemoryCacheImageInputStream(bais);
        ) {
            return readImageFromStream(memCache);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new ImageException("Cannot create stream from image bytes");
        }
    }

    public CustomImageWriter toWriter() {
        return new CustomImageWriter(this);
    }

    /**
     * Converts ImageInputStream to BufferedImage explicitly using correct reader
     *
     * @param is bytes
     * @return
     * @throws ImageException if image is not readable
     */
    private static ValuePair<BufferedImage, ImageMimeType> readImageFromStream(MemoryCacheImageInputStream is) throws IOException, ImageTypeNotSupportedException {
        BufferedImage image;
        final Iterator<ImageReader> it = ImageIO.getImageReaders(is);

        while (it.hasNext()) {
            final ImageReader reader = it.next();
            try {
                reader.setInput(is, true, true);

                SecurityUtilities.sanitizeImageMetadata(reader);

                final ImageMimeType imageMimeType = ImageMimeType.fromFormatString(reader.getFormatName());
                image = readImage(reader);

                return new ValuePair<>(image, imageMimeType);
            } catch (IOException | IllegalArgumentException ex) {
                log.warn("Failed to use reader", ex);
            } finally {
                reader.dispose();
            }
        }

        throw new ImageTypeNotSupportedException("No readers for the image stream found!");
    }

    /**
     * Config and use reader
     *
     * @param reader
     * @return
     * @throws IOException
     * @throws ImageTypeNotSupportedException
     */
    private static BufferedImage readImage(final ImageReader reader) throws IOException {
        final ImageReadParam params = reader.getDefaultReadParam();

        BufferedImage image = null;
        log.debug("Using reader {}", reader);
        image = reader.read(0, params);
        return image;
    }

}
